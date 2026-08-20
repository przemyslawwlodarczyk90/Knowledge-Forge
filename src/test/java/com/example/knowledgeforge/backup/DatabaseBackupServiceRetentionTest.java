package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.support.TestAppConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupServiceRetentionTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private void createDump(Path dir, LocalDateTime ts) throws IOException {
        Files.writeString(dir.resolve("knowledge-forge_" + FMT.format(ts) + ".dump"), "fake dump content");
    }

    @Test
    void retention_keeps_newest_and_recent_deletes_older_than_cutoff(@TempDir Path dir) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createDump(dir, now.minusDays(20)); // starszy niż 14 dni -> usunięty
        createDump(dir, now.minusDays(10)); // w granicach 14 dni -> zostaje
        createDump(dir, now);               // najnowszy -> zawsze zostaje

        DatabaseBackupService service = new DatabaseBackupService(TestAppConfigs.withBackupDir(dir, 14), new MaintenanceGate());
        service.applyRetention();

        List<DatabaseBackupService.BackupFileDto> remaining = service.listBackups();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().noneMatch(b -> b.fileName().contains(FMT.format(now.minusDays(20)))));
    }

    @Test
    void retention_never_deletes_the_single_newest_backup_even_if_older_than_retention(@TempDir Path dir) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createDump(dir, now.minusDays(40)); // jedyny plik -> jednocześnie najnowszy, mimo wieku

        DatabaseBackupService service = new DatabaseBackupService(TestAppConfigs.withBackupDir(dir, 14), new MaintenanceGate());
        service.applyRetention();

        assertEquals(1, service.listBackups().size(), "the single/newest backup must never be deleted, regardless of age");
    }

    @Test
    void retention_never_touches_files_outside_the_dump_naming_pattern(@TempDir Path dir) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createDump(dir, now); // najnowszy poprawny dump
        Files.writeString(dir.resolve("knowledge-forge_" + FMT.format(now.minusDays(40)) + ".dump.part"), "in progress");
        Files.writeString(dir.resolve("not-a-backup.txt"), "unrelated file");
        Files.writeString(dir.resolve("random.dump"), "wrong name pattern");

        DatabaseBackupService service = new DatabaseBackupService(TestAppConfigs.withBackupDir(dir, 14), new MaintenanceGate());
        service.applyRetention();

        assertTrue(Files.exists(dir.resolve("not-a-backup.txt")));
        assertTrue(Files.exists(dir.resolve("random.dump")));
        try (var walk = Files.walk(dir)) {
            assertTrue(walk.anyMatch(p -> p.getFileName().toString().endsWith(".dump.part")), ".part file must survive retention untouched");
        }
    }

    @Test
    void dumpFilenamePattern_matches_only_the_exact_knowledge_forge_format() {
        var pattern = DatabaseBackupService.dumpFilenamePattern();
        assertTrue(pattern.matcher("knowledge-forge_2026-08-20_02-00-00.dump").matches());
        assertFalse(pattern.matcher("knowledge-forge_2026-08-20_02-00-00.dump.part").matches());
        assertFalse(pattern.matcher("../etc/passwd").matches());
        assertFalse(pattern.matcher("knowledge-forge_2026-08-20.dump").matches());
        assertFalse(pattern.matcher("other-app_2026-08-20_02-00-00.dump").matches());
    }
}
