package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.support.TestAppConfigs;
import com.example.knowledgeforge.support.TestDatabase;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * resolveBackupFile() nie dotyka bazy w ogóle (czyste sprawdzenie nazwy pliku + normalize()
 * + containment) — HikariDataSource jest tu tylko dlatego, że konstruktor DatabaseRestoreService
 * go wymaga (na potrzeby ewentualnego suspend/resume puli podczas prawdziwego restore).
 * Wskazuje na osobną bazę testową (kf_test), nigdy na roboczą.
 */
class DatabaseRestoreServicePathTest {

    static HikariDataSource ds;

    @BeforeAll
    static void setUp() {
        ds = TestDatabase.create();
    }

    @AfterAll
    static void tearDown() {
        if (ds != null) ds.close();
    }

    private DatabaseRestoreService serviceFor(Path dir) {
        AppConfig config = TestAppConfigs.withBackupDir(dir);
        MaintenanceGate gate = new MaintenanceGate();
        DatabaseBackupService backupService = new DatabaseBackupService(config, gate);
        return new DatabaseRestoreService(config, gate, backupService, ds);
    }

    @Test
    void resolveBackupFile_accepts_existing_validly_named_file(@TempDir Path dir) throws Exception {
        String name = "knowledge-forge_2026-08-20_02-00-00.dump";
        Files.writeString(dir.resolve(name), "fake");

        Path resolved = serviceFor(dir).resolveBackupFile(name);
        assertEquals(dir.resolve(name).toRealPath(), resolved.toRealPath());
    }

    @Test
    void resolveBackupFile_rejects_path_traversal_attempts(@TempDir Path dir) {
        DatabaseRestoreService service = serviceFor(dir);
        assertThrows(ValidationException.class, () -> service.resolveBackupFile("../../etc/passwd"));
        assertThrows(ValidationException.class, () -> service.resolveBackupFile("..%2f..%2fetc%2fpasswd.dump"));
        assertThrows(ValidationException.class, () -> service.resolveBackupFile("2026/../../../etc/passwd.dump"));
    }

    @Test
    void resolveBackupFile_rejects_names_not_matching_the_strict_pattern(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("not-a-real-backup.dump"), "x");
        DatabaseRestoreService service = serviceFor(dir);
        assertThrows(ValidationException.class, () -> service.resolveBackupFile("not-a-real-backup.dump"));
        assertThrows(ValidationException.class, () -> service.resolveBackupFile(""));
        assertThrows(ValidationException.class, () -> service.resolveBackupFile(null));
    }

    @Test
    void resolveBackupFile_rejects_correctly_named_but_nonexistent_file(@TempDir Path dir) {
        DatabaseRestoreService service = serviceFor(dir);
        assertThrows(ValidationException.class, () ->
                service.resolveBackupFile("knowledge-forge_2026-08-20_02-00-00.dump"));
    }
}
