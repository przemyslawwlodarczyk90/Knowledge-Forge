package com.example.knowledgeforge.storage;

import com.example.knowledgeforge.domain.exception.PayloadTooLargeException;
import com.example.knowledgeforge.domain.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Jedyna klasa, która dotyka systemu plików dla załączników. Baza danych trzyma tylko
 * metadane i relativePath (zob. domain.attachment.Attachment) — ta klasa jest źródłem
 * prawdy o tym, JAK i GDZIE bajty faktycznie leżą na dysku.
 *
 * Zasady bezpieczeństwa (wymagane przy uploadzie):
 *  - oryginalna nazwa pliku NIGDY nie jest używana jako fizyczna nazwa — tylko do
 *    wyciągnięcia bezpiecznego rozszerzenia (safeExtension),
 *  - fizyczna nazwa to zawsze świeży UUID,
 *  - każda ścieżka po Path#normalize() jest sprawdzana, czy nadal leży wewnątrz baseDir,
 *  - plik jest najpierw zapisywany jako "*.part" (strumieniowo, bez byte[] całego pliku),
 *    a dopiero po policzeniu rozmiaru/SHA-256 przenoszony ATOMICALLY na docelową nazwę,
 *  - nigdy nie nadpisujemy istniejącego pliku (StandardCopyOption bez REPLACE_EXISTING).
 */
public class AttachmentStorage {

    private static final Logger log = Logger.getLogger(AttachmentStorage.class.getName());

    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,10}");
    private static final String DEFAULT_EXTENSION = "bin";
    private static final int HEADER_PEEK_BYTES = 16;
    private static final int COPY_BUFFER_SIZE = 8192;

    private final Path baseDir;

    public AttachmentStorage(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            // Wymóg konfiguracyjny: jeśli katalogu załączników nie da się utworzyć, aplikacja
            // ma jednoznacznie zalogować błąd i przerwać start — nie ma sensu wystawiać API,
            // które i tak nie potrafi zapisać żadnego pliku.
            log.severe("Failed to create attachments storage folder: " + this.baseDir + " -> " + e.getMessage());
            throw new UncheckedIOException("Failed to create attachments storage folder: " + this.baseDir, e);
        }
        log.info(() -> "Attachments storage folder: " + this.baseDir);
    }

    public Path baseDir() {
        return baseDir;
    }

    /** Wynik strumieniowego zapisu — wszystko, czego AttachmentService potrzebuje do zbudowania rekordu w bazie. */
    public record SaveResult(
            String relativePath,
            String storedName,
            long sizeBytes,
            String checksumSha256,
            byte[] header,
            int headerLength,
            String probedContentType
    ) {
    }

    /**
     * Zapisuje strumień uploadu na dysk: najpierw jako "*.part" w katalogu {@code yyyy/MM/},
     * licząc rozmiar i SHA-256 w locie (bez wczytywania całego pliku do pamięci), egzekwując
     * {@code maxBytes} podczas kopiowania, a na końcu atomowo przenosząc do docelowej nazwy.
     * W razie jakiegokolwiek błędu plik tymczasowy jest usuwany.
     */
    public SaveResult save(InputStream uploadStream, String originalName, long maxBytes) throws IOException {
        String extension = safeExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;

        LocalDate today = LocalDate.now();
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path dir = resolveWithinBase(yearMonth);
        Files.createDirectories(dir);

        Path finalPath = resolveWithinBase(yearMonth + "/" + storedName);
        Path tempPath = finalPath.resolveSibling(storedName + ".part");

        if (Files.exists(finalPath)) {
            // Astronomicznie nieprawdopodobne przy świeżym UUID, ale sprawdzamy jawnie —
            // nigdy nie wolno nadpisać istniejącego pliku.
            throw new IOException("Refusing to overwrite existing attachment file: " + finalPath);
        }

        MessageDigest digest = sha256();
        byte[] header = new byte[HEADER_PEEK_BYTES];
        int headerLength = 0;
        long total = 0;

        try {
            try (OutputStream out = Files.newOutputStream(tempPath,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buf = new byte[COPY_BUFFER_SIZE];
                int read;
                while ((read = uploadStream.read(buf)) != -1) {
                    total += read;
                    if (total > maxBytes) {
                        throw new PayloadTooLargeException(
                                "Uploaded file exceeds the configured limit of " + maxBytes + " bytes");
                    }
                    if (headerLength < header.length) {
                        int toCopy = Math.min(read, header.length - headerLength);
                        System.arraycopy(buf, 0, header, headerLength, toCopy);
                        headerLength += toCopy;
                    }
                    digest.update(buf, 0, read);
                    out.write(buf, 0, read);
                }
            }

            if (total == 0) {
                throw new ValidationException("Uploaded file is empty");
            }

            // Ta sama ochrona co przy odczycie — nawet ścieżkę, którą sami zbudowaliśmy
            // z bezpiecznych składników, weryfikujemy po normalize() przed jakąkolwiek operacją.
            assertWithinBase(finalPath);
            Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE);

            String checksum = HexFormat.of().formatHex(digest.digest());
            String probedContentType = probeContentTypeQuietly(finalPath);
            String relativePath = baseDir.relativize(finalPath).toString().replace('\\', '/');

            long finalTotal = total;
            log.info(() -> "Saved attachment file: " + relativePath + " (" + finalTotal + " bytes, sha256=" + checksum + ")");

            return new SaveResult(relativePath, storedName, total, checksum, header, headerLength, probedContentType);
        } catch (Exception e) {
            deleteQuietly(tempPath);
            deleteQuietly(finalPath);
            if (e instanceof IOException io) throw io;
            if (e instanceof RuntimeException re) throw re;
            throw new IOException("Failed to save attachment file", e);
        }
    }

    /** Otwiera plik do strumieniowego odczytu (download/view) — id/relativePath pochodzą wyłącznie z bazy, nigdy z URL-a wprost. */
    public Path resolveExisting(String relativePath) {
        Path path = resolveWithinBase(relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new java.io.UncheckedIOException(new java.io.FileNotFoundException(
                    "Attachment file missing on disk: " + relativePath));
        }
        return path;
    }

    /** Usuwa plik załącznika z dysku. Zwraca false (bez wyjątku), jeśli pliku już nie było — wywołujący loguje wg potrzeby. */
    public boolean delete(String relativePath) {
        Path path = resolveWithinBase(relativePath);
        try {
            boolean existed = Files.deleteIfExists(path);
            if (existed) {
                log.info(() -> "Deleted attachment file: " + relativePath);
            } else {
                log.warning(() -> "Attachment file already missing, nothing to delete: " + relativePath);
            }
            return existed;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to delete attachment file: " + relativePath, e);
            return false;
        }
    }

    /** true jeśli plik istnieje na dysku pod danym relativePath — używane przez diagnostykę spójności. */
    public boolean exists(String relativePath) {
        try {
            Path path = resolveWithinBase(relativePath);
            return Files.isRegularFile(path);
        } catch (ValidationException e) {
            return false;
        }
    }

    // ── Ochrona przed path traversal ─────────────────────────────────

    private Path resolveWithinBase(String relativePath) {
        Path candidate = baseDir.resolve(relativePath).normalize();
        assertWithinBase(candidate);
        return candidate;
    }

    private void assertWithinBase(Path candidate) {
        if (!candidate.startsWith(baseDir)) {
            log.severe(() -> "Path traversal attempt blocked: resolved '" + candidate + "' is outside " + baseDir);
            throw new ValidationException("Invalid attachment path");
        }
    }

    // ── Pomocnicze ────────────────────────────────────────────────────

    /** Tylko [a-z0-9], max 10 znaków — używane WYŁĄCZNIE do budowy fizycznej nazwy na dysku. */
    static String safeExtension(String originalName) {
        if (originalName == null) return DEFAULT_EXTENSION;
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) return DEFAULT_EXTENSION;
        String ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(ext).matches() ? ext : DEFAULT_EXTENSION;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    private static String probeContentTypeQuietly(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            log.fine(() -> "probeContentType failed for " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warning(() -> "Failed to clean up temp/partial attachment file " + path + ": " + e.getMessage());
        }
    }
}
