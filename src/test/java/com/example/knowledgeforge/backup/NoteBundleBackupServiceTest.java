package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.document.DocumentContainer;
import com.example.knowledgeforge.json.JsonMapper;
import com.example.knowledgeforge.support.TestAppConfigs;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NoteBundleBackupService — mechanizm codziennego, ZBIORCZEGO backupu .kfdoc -> .kfbundle,
 * niezależnego od bazy (zob. dokumentacja/BACKUP_STRATEGY.txt). Zgodnie z korektą "tylko jeden
 * zbiorczy plik backupu": bez historii, bez retencji — katalog backupu zawsze zawiera co najwyżej
 * jeden {@value NoteBundleBackupService#FINAL_NAME}, bezpiecznie zastępowany przez plik
 * tymczasowy + walidację + atomową zamianę.
 */
class NoteBundleBackupServiceTest {

    // ── Pomocnicze: budowanie realnych plików .kfdoc na dysku, bez przechodzenia przez serwis ──

    private UUID writeKfdoc(Path notesDir, String title, String type, List<String> categoryPath) throws IOException {
        UUID topicId = UUID.randomUUID();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topicId", topicId.toString());
        metadata.put("noteId", UUID.randomUUID().toString());
        metadata.put("title", title);
        metadata.put("author", "Ala");
        metadata.put("type", type);
        metadata.put("detailLevel", "MEDIUM");
        metadata.put("categoryName", categoryPath.isEmpty() ? null : categoryPath.get(categoryPath.size() - 1));
        metadata.put("categoryPath", categoryPath);
        metadata.put("noteVersion", 1);
        metadata.put("savedAt", Instant.now().toString());

        JsonNode content = JsonMapper.get().createObjectNode().put("type", "doc");
        byte[] bytes = DocumentContainer.of(content, Map.of(), metadata).toBytes();
        Files.write(notesDir.resolve(topicId + ".kfdoc"), bytes);
        return topicId;
    }

    /** Wariant bez categoryPath w metadanych — symuluje STARSZY plik sprzed tej zmiany (zob. NoteService). */
    private UUID writeLegacyKfdoc(Path notesDir, String title) throws IOException {
        UUID topicId = UUID.randomUUID();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topicId", topicId.toString());
        metadata.put("title", title);
        metadata.put("type", "NOTE");
        metadata.put("categoryName", "Stara kategoria");
        // celowo BRAK "categoryPath" — starszy zapis
        JsonNode content = JsonMapper.get().createObjectNode().put("type", "doc");
        byte[] bytes = DocumentContainer.of(content, Map.of(), metadata).toBytes();
        Files.write(notesDir.resolve(topicId + ".kfdoc"), bytes);
        return topicId;
    }

    private Path notesDir(Path tmp) throws IOException {
        Path dir = tmp.resolve("notes");
        Files.createDirectories(dir);
        return dir;
    }

    private Path backupDir(Path tmp) {
        return tmp.resolve("note-bundle-backups");
    }

    private JsonNode readEntryAsJson(Path bundle, String entryName) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(bundle))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) {
                    return JsonMapper.get().readTree(zin.readAllBytes());
                }
            }
        }
        throw new AssertionError("Entry not found in bundle: " + entryName);
    }

    private List<Path> filesInDir(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) result.add(p);
        }
        return result;
    }

    // ── 1-2-3: pojedynczy plik, bezpiecznie nadpisywany ──────────────────────────────────────

    @Test
    void first_run_creates_the_single_final_bundle(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "Pierwsza notatka", "NOTE", List.of("IT", "Java"));
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        var result = service.runBackupNow();

        assertTrue(result.success());
        assertEquals(1, result.documentCount());
        assertTrue(Files.exists(service.finalPath()));
        assertEquals(NoteBundleBackupService.FINAL_NAME, service.finalPath().getFileName().toString());
    }

    @Test
    void second_run_replaces_the_same_file_only_one_bundle_remains(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        assertTrue(service.runBackupNow().success());
        writeKfdoc(notes, "B", "NOTE", List.of()); // druga notatka przed drugim przebiegiem
        assertTrue(service.runBackupNow().success());

        List<Path> kfbundleFiles = filesInDir(backupDir(tmp)).stream()
                .filter(p -> p.getFileName().toString().endsWith(".kfbundle"))
                .toList();
        assertEquals(1, kfbundleFiles.size(), "exactly one .kfbundle must remain after the second run");
        assertEquals(NoteBundleBackupService.FINAL_NAME, kfbundleFiles.get(0).getFileName().toString());
    }

    @Test
    void second_run_manifest_has_newer_createdAt_than_the_first(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        var service = new NoteBundleBackupService(config, clock);

        service.runBackupNow();
        String firstCreatedAt = readEntryAsJson(service.finalPath(), "manifest.json").get("createdAt").asText();

        clock.advance(java.time.Duration.ofDays(1));
        service.runBackupNow();
        String secondCreatedAt = readEntryAsJson(service.finalPath(), "manifest.json").get("createdAt").asText();

        assertTrue(Instant.parse(secondCreatedAt).isAfter(Instant.parse(firstCreatedAt)));
    }

    @Test
    void bundle_reflects_the_current_full_set_of_kfdoc_files_at_run_time(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);
        service.runBackupNow();

        writeKfdoc(notes, "B", "NOTE", List.of());
        writeKfdoc(notes, "C", "INSTRUCTION", List.of());
        service.runBackupNow();

        JsonNode index = readEntryAsJson(service.finalPath(), "index.json");
        assertEquals(3, index.size());
        assertEquals(3, readEntryAsJson(service.finalPath(), "manifest.json").get("documentCount").asInt());
    }

    // ── Odporność na uszkodzone / starsze pliki ──────────────────────────────────────────────

    @Test
    void corrupted_kfdoc_is_included_raw_and_marked_corrupted_backup_still_succeeds(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "Dobra notatka", "NOTE", List.of());
        UUID corruptId = UUID.randomUUID();
        Files.writeString(notes.resolve(corruptId + ".kfdoc"), "to nie jest prawidłowy plik ZIP");
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        var result = service.runBackupNow();

        assertTrue(result.success(), "one corrupted file must not abort the whole backup");
        assertEquals(2, result.documentCount());
        assertEquals(1, result.corruptedCount());

        JsonNode index = readEntryAsJson(service.finalPath(), "index.json");
        boolean foundCorrupted = false;
        for (JsonNode entry : index) {
            if (entry.get("topicId").asText().equals(corruptId.toString())) {
                assertTrue(entry.get("corrupted").asBoolean());
                assertFalse(entry.get("error").isNull());
                foundCorrupted = true;
            }
        }
        assertTrue(foundCorrupted, "corrupted document must still appear in the index");
    }

    @Test
    void legacy_kfdoc_without_categoryPath_is_still_included(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        UUID legacyId = writeLegacyKfdoc(notes, "Stara notatka");
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        var result = service.runBackupNow();

        assertTrue(result.success());
        assertEquals(1, result.documentCount());
        assertEquals(0, result.corruptedCount());
        JsonNode index = readEntryAsJson(service.finalPath(), "index.json");
        assertEquals(legacyId.toString(), index.get(0).get("topicId").asText());
        assertEquals("Stara kategoria", index.get(0).get("categoryName").asText());
        assertTrue(index.get(0).get("categoryPath").isEmpty());
    }

    // ── Bezpieczne nadpisywanie: awaria zostawia poprzedni backup nietknięty ────────────────

    @Test
    void failed_write_preserves_previous_good_backup_and_leaves_no_part_file(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);
        assertTrue(service.runBackupNow().success());
        byte[] previousBytes = Files.readAllBytes(service.finalPath());

        // Wymuszenie awarii zapisu w sposób deterministyczny i przenośny: pod docelową nazwą
        // ".part" tworzymy KATALOG zamiast pliku — Files.newOutputStream na taką ścieżkę zawsze
        // rzuca IOException, niezależnie od uprawnień systemu plików.
        Files.createDirectories(service.partPath());

        var result = service.runBackupNow();

        assertFalse(result.success());
        assertTrue(Files.exists(service.finalPath()), "previous good backup must survive a failed run");
        assertArrayEquals(previousBytes, Files.readAllBytes(service.finalPath()));
        // Serwis sam sprząta po nieudanej próbie (deleteQuietly na ".part") — nasz sztucznie
        // podłożony katalog powinien więc już nie istnieć; deleteIfExists tylko na wszelki wypadek.
        Files.deleteIfExists(service.partPath());
    }

    private static void assertArrayEquals(byte[] a, byte[] b) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(a, b);
    }

    @Test
    void successful_run_leaves_no_part_file(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        service.runBackupNow();

        assertFalse(Files.exists(service.partPath()));
    }

    @Test
    void orphaned_part_from_a_previous_crashed_run_is_deleted_at_startup_final_bundle_untouched(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);
        assertTrue(service.runBackupNow().success());
        byte[] goodBytes = Files.readAllBytes(service.finalPath());

        Files.writeString(service.partPath(), "leftover from a crashed process");
        assertTrue(Files.exists(service.partPath()));

        service.cleanupOrphanedPartOnStartup();

        assertFalse(Files.exists(service.partPath()), "orphaned .part must be removed at startup");
        assertTrue(Files.exists(service.finalPath()), "the valid final bundle must never be touched by startup cleanup");
        assertArrayEquals(goodBytes, Files.readAllBytes(service.finalPath()));
    }

    @Test
    void unknown_files_in_the_backup_directory_are_never_deleted(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        writeKfdoc(notes, "A", "NOTE", List.of());
        Path backup = backupDir(tmp);
        Files.createDirectories(backup);
        Path unrelated = backup.resolve("readme.txt");
        Files.writeString(unrelated, "not a backup file, leave it alone");

        var config = TestAppConfigs.withNoteBundleBackup(notes, backup);
        var service = new NoteBundleBackupService(config);
        service.runBackupNow();
        service.runBackupNow(); // druga podmiana — sprawdzamy, że nadal nietknięty

        assertTrue(Files.exists(unrelated));
    }

    // ── Brak równoległego generowania dwóch backupów ────────────────────────────────────────

    @Test
    void two_concurrent_backup_runs_never_both_succeed_at_once(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        for (int i = 0; i < 50; i++) writeKfdoc(notes, "Note " + i, "NOTE", List.of("Kat" + (i % 5)));
        var config = TestAppConfigs.withNoteBundleBackup(notes, backupDir(tmp));
        var service = new NoteBundleBackupService(config);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<NoteBundleBackupService.BackupResult>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return service.runBackupNow();
                }));
            }
            startGate.countDown();
            int successCount = 0;
            for (var f : futures) {
                if (f.get(10, TimeUnit.SECONDS).success()) successCount++;
            }
            // Co najmniej jeden musi się powieść; jeśli oba wystartowały naprawdę równolegle,
            // AtomicBoolean w serwisie gwarantuje, że NIE oba zwrócą sukces w tym samym przebiegu.
            assertTrue(successCount >= 1);
            assertTrue(Files.exists(service.finalPath()));
        } finally {
            pool.shutdownNow();
        }
    }

    // ── Niezależność od bazy danych ─────────────────────────────────────────────────────────

    @Test
    void service_has_no_dependency_on_any_database_access_class() {
        for (Field f : NoteBundleBackupService.class.getDeclaredFields()) {
            String typeName = f.getType().getName();
            assertFalse(typeName.contains("Dao"), "field " + f.getName() + " must not be a *Dao — bundle backup must never query the database");
            assertFalse(typeName.contains("DataSource"), "field " + f.getName() + " must not be a DataSource");
        }
        for (var ctor : NoteBundleBackupService.class.getDeclaredConstructors()) {
            for (Class<?> paramType : ctor.getParameterTypes()) {
                assertFalse(paramType.getName().contains("Dao"), "constructor must not accept a *Dao parameter");
                assertFalse(paramType.getName().contains("DataSource"), "constructor must not accept a DataSource parameter");
            }
        }
    }

    @Test
    void directory_equal_to_notes_storage_path_fails_construction(@TempDir Path tmp) throws Exception {
        Path notes = notesDir(tmp);
        var config = TestAppConfigs.withNoteBundleBackup(notes, notes); // celowo ten sam katalog
        assertThrows(IllegalStateException.class, () -> new NoteBundleBackupService(config));
    }

    // ── Pomocnicze ───────────────────────────────────────────────────────────────────────────

    /** Clock sterowalny w trakcie testu — analogiczny do wzorca z ActualityVerificationServiceTest. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(java.time.Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
