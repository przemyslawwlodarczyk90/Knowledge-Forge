package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.json.JsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Codzienny, ZBIORCZY, awaryjny backup wszystkich plików {@code .kfdoc} (notatek i instrukcji) —
 * całkowicie NIEZALEŻNY od bazy danych. Skanuje {@code notes.storage.path} BEZPOŚREDNIO Z DYSKU
 * (nigdy tabelę {@code note}), więc znajduje każdy fizycznie istniejący {@code .kfdoc} nawet gdy
 * w bazie brakuje rekordu, {@code content_path} jest błędny, tabela {@code note}/{@code topic}
 * została usunięta, albo cała baza jest pusta/uszkodzona. Zob. dokumentacja/BACKUP_STRATEGY.txt.
 *
 * W katalogu backupu ({@code note-bundle-backup.directory}) istnieje ZAWSZE co najwyżej jeden
 * właściwy plik: {@value #FINAL_NAME} — BEZ historii, BEZ retencji, BEZ nazw z datą w środku.
 * Każde uruchomienie buduje nową wersję i BEZPIECZNIE ją podmienia (plik tymczasowy ({@value
 * #PART_NAME}) -> pełny zapis -> zamknięcie ZIP-a -> walidacja odczytu -> ATOMIC_MOVE z
 * REPLACE_EXISTING) — nigdy nie zapisuje wprost do pliku docelowego, żeby awaria w trakcie zapisu
 * nie zniszczyła jedynej istniejącej kopii. Nieudane generowanie/walidacja: usuwa {@code .part},
 * zostawia poprzedni poprawny backup nietknięty, loguje błąd.
 */
public class NoteBundleBackupService {

    private static final Logger log = Logger.getLogger(NoteBundleBackupService.class.getName());

    public static final String FINAL_NAME = "knowledge-forge-notes-latest.kfbundle";
    public static final String PART_NAME = FINAL_NAME + ".part";
    private static final String GENERATOR = "Knowledge Forge 2.0";
    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final String INDEX_ENTRY = "index.json";
    private static final String DOCUMENTS_PREFIX = "documents/";

    private final Path notesDir;
    private final Path backupDir;
    private final Clock clock;

    /** Chroni przed równoległym uruchomieniem dwóch backupów naraz (harmonogram + ewentualny ręczny trigger). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NoteBundleBackupService(AppConfig config) {
        this(config, Clock.systemUTC());
    }

    /** Wariant z wstrzykiwanym {@link Clock} — używany przez testy (deterministyczne createdAt w manifeście). */
    public NoteBundleBackupService(AppConfig config, Clock clock) {
        this.clock = clock;
        this.notesDir = Path.of(config.notesStoragePath()).toAbsolutePath().normalize();
        this.backupDir = Path.of(config.noteBundleBackupDirectory()).toAbsolutePath().normalize();
        if (this.backupDir.equals(this.notesDir)) {
            throw new IllegalStateException(
                    "note-bundle-backup.directory must not be the same directory as notes.storage.path "
                            + "(both resolve to " + this.backupDir + ") — a bundle backup living inside the "
                            + "directory it protects is not a backup.");
        }
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create note bundle backup directory: " + backupDir, e);
        }
        log.info(() -> "Note bundle backup directory: " + backupDir);
        warnIfSameFilesystem();
    }

    private void warnIfSameFilesystem() {
        try {
            Object notesStore = Files.getFileStore(Files.createDirectories(notesDir));
            Object backupStore = Files.getFileStore(backupDir);
            if (notesStore.equals(backupStore)) {
                log.warning(() -> "note-bundle-backup.directory (" + backupDir + ") is on the SAME physical "
                        + "disk/volume as notes.storage.path (" + notesDir + ") — a full-disk failure would "
                        + "destroy the backup together with the originals. A separate volume or network "
                        + "share is strongly recommended (zob. dokumentacja/BACKUP_STRATEGY.txt).");
            }
        } catch (IOException e) {
            log.fine(() -> "Could not compare file stores of " + notesDir + " and " + backupDir + ": " + e.getMessage());
        }
    }

    public Path backupDir() {
        return backupDir;
    }

    public Path finalPath() {
        return backupDir.resolve(FINAL_NAME);
    }

    public Path partPath() {
        return backupDir.resolve(PART_NAME);
    }

    /**
     * Usuwa osierocony {@code .part} (przerwany proces w trakcie poprzedniego zapisu) — wołane
     * raz, przy starcie aplikacji, PRZED startem harmonogramu. Plik {@code .part} NIGDY nie jest
     * traktowany jako coś, co dałoby się odzyskać — jedynym poprawnym backupem jest zawsze pełny,
     * zwalidowany {@value #FINAL_NAME}.
     */
    public void cleanupOrphanedPartOnStartup() {
        Path part = partPath();
        try {
            if (Files.deleteIfExists(part)) {
                log.warning(() -> "Removed orphaned " + PART_NAME + " left over from an interrupted run: " + part);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, e, () -> "Failed to remove orphaned " + PART_NAME + ": " + part);
        }
    }

    public record BackupResult(boolean success, int documentCount, int corruptedCount, String message) {
    }

    /** Backup na żądanie — wołane przez scheduler (zob. NoteBundleBackupScheduler). */
    public BackupResult runBackupNow() {
        if (!running.compareAndSet(false, true)) {
            log.warning("Note bundle backup skipped: another run is already in progress");
            return new BackupResult(false, 0, 0, "Another backup run is already in progress");
        }
        try {
            return doBackup();
        } finally {
            running.set(false);
        }
    }

    private BackupResult doBackup() {
        Path part = partPath();
        List<DocumentEntry> entries;
        try {
            entries = scanDocuments();
        } catch (IOException e) {
            log.log(Level.SEVERE, e, () -> "Failed to scan notes directory: " + notesDir);
            return new BackupResult(false, 0, 0, "Failed to scan notes directory: " + e.getMessage());
        }

        int corruptedCount = (int) entries.stream().filter(DocumentEntry::corrupted).count();
        Instant createdAt = Instant.now(clock);

        try {
            writeBundle(part, entries, createdAt);
            validate(part, entries.size());
            moveAtomically(part, finalPath());
        } catch (IOException | RuntimeException e) {
            deleteQuietly(part);
            log.log(Level.SEVERE, e, () -> "Note bundle backup failed — previous good backup (if any) left untouched");
            return new BackupResult(false, 0, 0, "Note bundle backup failed: " + e.getMessage());
        }

        int total = entries.size();
        log.info(() -> "Note bundle backup finished — " + total + " document(s), " + corruptedCount + " corrupted, -> " + finalPath());
        return new BackupResult(true, total, corruptedCount, "OK");
    }

    // ── Skanowanie katalogu notatek — WYŁĄCZNIE z dysku, zero zapytań do bazy ──────────────────

    private List<DocumentEntry> scanDocuments() throws IOException {
        List<DocumentEntry> result = new ArrayList<>();
        if (!Files.isDirectory(notesDir)) return result;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(notesDir, "*.kfdoc")) {
            for (Path path : stream) {
                if (!Files.isRegularFile(path)) continue;
                result.add(readDocument(path));
            }
        }
        result.sort(DOCUMENT_ORDER);
        return result;
    }

    @SuppressWarnings("unchecked")
    private DocumentEntry readDocument(Path path) {
        String fileName = path.getFileName().toString();
        byte[] raw;
        try {
            raw = Files.readAllBytes(path);
        } catch (IOException e) {
            // Plik zniknął / nieczytelny między listowaniem katalogu a odczytem — pomijamy, ale
            // NIE przerywamy całego backupu (zob. klasowy komentarz: lepiej za dużo niż za mało).
            log.warning(() -> "Skipping unreadable file during bundle scan: " + path + " (" + e.getMessage() + ")");
            return new DocumentEntry(fileName, raw(new byte[0]), null, true, "Unreadable file: " + e.getMessage());
        }

        String sha256 = sha256Hex(raw);
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(raw))) {
            Map<String, Object> manifest = null;
            boolean sawContent = false;
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().equals("manifest.json")) {
                    manifest = JsonMapper.get().readValue(zin.readAllBytes(), Map.class);
                } else if (entry.getName().equals("content.json")) {
                    sawContent = true;
                    zin.readAllBytes();
                }
                zin.closeEntry();
            }
            if (manifest == null || !sawContent) {
                return new DocumentEntry(fileName, raw(raw, sha256), null, true,
                        "Missing manifest.json or content.json inside the .kfdoc container");
            }
            Object metaObj = manifest.get("metadata");
            Map<String, Object> meta = metaObj instanceof Map ? (Map<String, Object>) metaObj : Map.of();
            return new DocumentEntry(fileName, raw(raw, sha256), meta, false, null);
        } catch (IOException | RuntimeException e) {
            return new DocumentEntry(fileName, raw(raw, sha256), null, true,
                    "Corrupted or unreadable .kfdoc container: " + e.getMessage());
        }
    }

    private static RawFile raw(byte[] bytes) {
        return raw(bytes, sha256Hex(bytes));
    }

    private static RawFile raw(byte[] bytes, String sha256) {
        return new RawFile(bytes, sha256);
    }

    private record RawFile(byte[] bytes, String sha256) {
    }

    private record DocumentEntry(String fileName, RawFile raw, Map<String, Object> metadata,
                                  boolean corrupted, String error) {
        String topicId() {
            int dot = fileName.indexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }

        String meta(String key) {
            Object v = metadata == null ? null : metadata.get(key);
            return v == null ? null : String.valueOf(v);
        }

        @SuppressWarnings("unchecked")
        List<String> categoryPath() {
            if (metadata == null) return List.of();
            Object v = metadata.get("categoryPath");
            if (v instanceof List) {
                List<String> path = new ArrayList<>();
                for (Object o : (List<Object>) v) path.add(String.valueOf(o));
                return path;
            }
            return List.of();
        }

        String sortKey() {
            List<String> path = categoryPath();
            String categoryKey = path.isEmpty() ? String.valueOf(meta("categoryName")) : String.join("/", path);
            return categoryKey + " " + meta("type") + " " + meta("title") + " " + topicId();
        }
    }

    /** Sortowanie stabilne: ścieżka/nazwa kategorii, typ, tytuł, topicId — zob. klasowy komentarz. */
    private static final Comparator<DocumentEntry> DOCUMENT_ORDER =
            Comparator.comparing(d -> d.sortKey() == null ? "" : d.sortKey());

    // ── Budowa i zapis archiwum ──────────────────────────────────────────────────────────────

    private void writeBundle(Path target, List<DocumentEntry> entries, Instant createdAt) throws IOException {
        try (var out = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            putEntry(zip, MANIFEST_ENTRY, buildManifest(entries, createdAt));
            putEntry(zip, INDEX_ENTRY, buildIndex(entries));
            for (DocumentEntry e : entries) {
                putEntry(zip, DOCUMENTS_PREFIX + e.fileName(), e.raw().bytes());
            }
        }
    }

    private byte[] buildManifest(List<DocumentEntry> entries, Instant createdAt) throws IOException {
        ObjectNode manifest = JsonMapper.get().createObjectNode();
        manifest.put("format", "kfbundle");
        manifest.put("version", 1);
        manifest.put("createdAt", createdAt.toString());
        manifest.put("documentCount", entries.size());
        manifest.put("corruptedCount", (int) entries.stream().filter(DocumentEntry::corrupted).count());
        manifest.put("generator", GENERATOR);
        return JsonMapper.get().writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
    }

    private byte[] buildIndex(List<DocumentEntry> entries) throws IOException {
        ArrayNode index = JsonMapper.get().createArrayNode();
        for (DocumentEntry e : entries) {
            ObjectNode node = index.addObject();
            node.put("fileName", DOCUMENTS_PREFIX + e.fileName());
            node.put("topicId", e.topicId());
            putNullable(node, "noteId", e.meta("noteId"));
            putNullable(node, "title", e.meta("title"));
            putNullable(node, "author", e.meta("author"));
            putNullable(node, "type", e.meta("type"));
            putNullable(node, "detailLevel", e.meta("detailLevel"));
            putNullable(node, "categoryName", e.meta("categoryName"));
            ArrayNode pathNode = node.putArray("categoryPath");
            e.categoryPath().forEach(pathNode::add);
            putNullable(node, "savedAt", e.meta("savedAt"));
            putNullable(node, "noteVersion", e.meta("noteVersion"));
            node.put("sizeBytes", e.raw().bytes().length);
            node.put("sha256", e.raw().sha256());
            node.put("corrupted", e.corrupted());
            putNullable(node, "error", e.error());
        }
        return JsonMapper.get().writerWithDefaultPrettyPrinter().writeValueAsBytes(index);
    }

    private static void putNullable(ObjectNode node, String field, String value) {
        if (value == null) node.putNull(field); else node.put(field, value);
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    /**
     * Walidacja PRZED atomową zamianą: manifest, indeks, liczba dokumentów, i faktyczna
     * możliwość ponownego odczytania archiwum jako ZIP-u od początku do końca.
     */
    private void validate(Path bundlePath, int expectedDocumentCount) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(bundlePath))) {
            boolean sawManifest = false;
            boolean sawIndex = false;
            int documentsSeen = 0;
            JsonNode manifestNode = null;
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (entry.getName().equals(MANIFEST_ENTRY)) {
                    sawManifest = true;
                    manifestNode = JsonMapper.get().readTree(data);
                } else if (entry.getName().equals(INDEX_ENTRY)) {
                    sawIndex = true;
                    JsonNode idx = JsonMapper.get().readTree(data);
                    if (!idx.isArray()) throw new IOException("index.json is not an array");
                } else if (entry.getName().startsWith(DOCUMENTS_PREFIX)) {
                    documentsSeen++;
                }
                zin.closeEntry();
            }
            if (!sawManifest) throw new IOException("Generated bundle is missing manifest.json");
            if (!sawIndex) throw new IOException("Generated bundle is missing index.json");
            if (!"kfbundle".equals(manifestNode.path("format").asText(null))) {
                throw new IOException("Generated bundle manifest has unexpected format: " + manifestNode.path("format"));
            }
            if (documentsSeen != expectedDocumentCount) {
                throw new IOException("Generated bundle has " + documentsSeen + " document(s), expected " + expectedDocumentCount);
            }
        }
    }

    private static void moveAtomically(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicNotSupported) {
            Files.move(from, to, (CopyOption) StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.log(Level.WARNING, e, () -> "Failed to remove leftover " + PART_NAME + ": " + path);
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
