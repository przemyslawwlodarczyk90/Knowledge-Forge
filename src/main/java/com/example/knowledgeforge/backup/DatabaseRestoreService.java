package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * pg_restore przez ProcessBuilder, z kontrolowanym maintenance mode wokół niego.
 *
 * Problem, który to rozwiązuje: HikariCP trzyma aktywną pulę połączeń do bazy przez cały
 * czas działania aplikacji. Odpalenie pg_restore (z --clean, czyli DROP+CREATE obiektów)
 * podczas gdy inne żądania HTTP równolegle czytają/piszą przez tę samą pulę, może:
 *  (a) zderzyć się z blokadami trzymanymi przez te połączenia,
 *  (b) zostawić aplikację operującą na "widmowych" połączeniach wskazujących na obiekty,
 *      które pg_restore właśnie skasował i odtworzył od nowa.
 *
 * Rozwiązanie — w tej kolejności:
 *  1. wzajemne wykluczenie z backupem (MaintenanceGate#tryAcquireOperation),
 *  2. wejście w maintenance mode — MaintenanceModeFilter zaczyna odrzucać nowe żądania
 *     modyfikujące dane (503),
 *  3. czekanie (z timeoutem) aż żądania już w locie się skończą,
 *  4. AWARYJNY backup obecnej bazy — jeśli się nie powiedzie, restore jest przerywany,
 *  5. zawieszenie puli HikariCP (suspendPool + softEvictConnections) — wymaga
 *     allowPoolSuspension=true, ustawionego w config.Database,
 *  6. pg_restore,
 *  7. próba bezpiecznego wznowienia puli + walidacja (SELECT 1 na świeżym połączeniu).
 *
 * Jeśli krok 7 się nie powiedzie, NIE udajemy sukcesu — aplikacja trwale przechodzi w stan
 * "wymagany restart" (MaintenanceGate#markAwaitingRestart): wszystkie kolejne żądania (także
 * GET) dostają 503, dopóki proces nie zostanie ręcznie zrestartowany. To świadomy wybór
 * architektoniczny, nie niedopatrzenie — przy obecnym composition roocie (Main.java) DAO
 * trzymają referencję do tej samej instancji HikariDataSource przez cały cykl życia procesu;
 * nie ma tu kontenera DI, który pozwoliłby bezpiecznie podmienić pulę pod spodem "na żywca".
 */
public class DatabaseRestoreService {

    private static final Logger log = Logger.getLogger(DatabaseRestoreService.class.getName());
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(30);

    private final AppConfig config;
    private final PgConnectionInfo connInfo;
    private final MaintenanceGate gate;
    private final DatabaseBackupService backupService;
    private final HikariDataSource dataSource;
    private final Path backupDir;

    public DatabaseRestoreService(AppConfig config, MaintenanceGate gate,
                                   DatabaseBackupService backupService, HikariDataSource dataSource) {
        this.config = config;
        this.gate = gate;
        this.backupService = backupService;
        this.dataSource = dataSource;
        this.connInfo = PgConnectionInfo.parse(config.dbUrl());
        this.backupDir = backupService.backupDir();
    }

    public enum Outcome {
        SUCCESS, SUCCESS_RESTART_REQUIRED, CONFLICT, EMERGENCY_BACKUP_FAILED, RESTORE_FAILED
    }

    public record RestoreResult(Outcome outcome, String message) {
    }

    /** Waliduje, że fileName pasuje do ścisłego wzorca backupu i istnieje wewnątrz backupDir. Rzuca ValidationException (400) w przeciwnym razie. */
    public Path resolveBackupFile(String fileName) {
        if (fileName == null || !DatabaseBackupService.dumpFilenamePattern().matcher(fileName).matches()) {
            throw new ValidationException("Invalid backup file name");
        }
        Path candidate = backupDir.resolve(fileName).normalize();
        if (!candidate.startsWith(backupDir) || !Files.isRegularFile(candidate)) {
            throw new ValidationException("Backup file not found: " + fileName);
        }
        return candidate;
    }

    public RestoreResult restore(String fileName) {
        Path dumpPath = resolveBackupFile(fileName);

        if (!gate.tryAcquireOperation()) {
            return new RestoreResult(Outcome.CONFLICT, "A backup or restore is already in progress");
        }
        try {
            gate.enterMaintenance();
            log.warning(() -> "Entering maintenance mode for restore from " + fileName);

            if (!gate.awaitDrain(DRAIN_TIMEOUT)) {
                gate.exitMaintenance();
                log.severe("Restore aborted: timed out waiting for in-flight write requests to finish");
                return new RestoreResult(Outcome.CONFLICT, "Timed out waiting for in-flight requests to finish");
            }

            // Krok wykonywany na tym samym wątku -> operationLock (ReentrantLock) jest reentrantny,
            // więc backupService.runBackupNow() bezpiecznie "dokłada się" do już trzymanej blokady.
            DatabaseBackupService.BackupResult emergency = backupService.runBackupNow();
            if (!emergency.success()) {
                gate.exitMaintenance();
                log.severe("Restore aborted: emergency pre-restore backup failed: " + emergency.message());
                return new RestoreResult(Outcome.EMERGENCY_BACKUP_FAILED,
                        "Pre-restore safety backup failed — restore aborted, database left untouched");
            }
            log.info(() -> "Emergency pre-restore backup created: " + emergency.fileName());

            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            boolean suspended = suspendPoolQuietly(pool);

            boolean restoreOk = runPgRestore(dumpPath);

            if (!restoreOk) {
                if (suspended) resumePoolQuietly(pool);
                gate.exitMaintenance();
                return new RestoreResult(Outcome.RESTORE_FAILED,
                        "pg_restore failed — see server logs. Emergency backup " + emergency.fileName() + " is available.");
            }

            boolean recovered = suspended && resumeAndValidate(pool);
            if (recovered) {
                gate.exitMaintenance();
                log.info("Restore completed successfully — connection pool recovered, no restart required");
                return new RestoreResult(Outcome.SUCCESS, "Restore completed successfully");
            }

            gate.markAwaitingRestart();
            log.severe("Restore completed but the connection pool could not be safely validated afterwards — "
                    + "APPLICATION RESTART REQUIRED. All further requests will return 503 until the process is restarted.");
            return new RestoreResult(Outcome.SUCCESS_RESTART_REQUIRED,
                    "Restore completed successfully, but the application must be restarted before continuing to use it");
        } finally {
            gate.releaseOperation();
        }
    }

    private boolean runPgRestore(Path dumpPath) {
        List<String> command = List.of(
                config.backupPgRestorePath(),
                "-h", connInfo.host(),
                "-p", String.valueOf(connInfo.port()),
                "-U", config.dbUsername(),
                "-d", connInfo.database(),
                "--clean", "--if-exists", "--no-owner",
                dumpPath.toString()
        );
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", config.dbPassword());

        try {
            ProcessRunner.Outcome outcome = ProcessRunner.run(pb);
            String stderr = ProcessRunner.scrub(outcome.stderr(), config.dbPassword());
            if (outcome.exitCode() != 0) {
                log.severe("pg_restore failed (exit=" + outcome.exitCode() + "): " + stderr);
                return false;
            }
            if (!stderr.isBlank()) {
                log.fine(() -> "pg_restore stderr (non-fatal): " + stderr);
            }
            log.info(() -> "pg_restore completed successfully from " + dumpPath.getFileName());
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "pg_restore execution failed", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.severe("pg_restore execution interrupted");
            return false;
        }
    }

    private boolean suspendPoolQuietly(HikariPoolMXBean pool) {
        try {
            pool.suspendPool();
            pool.softEvictConnections();
            log.info("Connection pool suspended for restore");
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to suspend connection pool before restore — proceeding anyway", e);
            return false;
        }
    }

    private void resumePoolQuietly(HikariPoolMXBean pool) {
        try {
            pool.resumePool();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to resume connection pool", e);
        }
    }

    private boolean resumeAndValidate(HikariPoolMXBean pool) {
        try {
            pool.resumePool();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to resume connection pool after restore", e);
            return false;
        }
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("SELECT 1");
            log.info("Post-restore connection pool validation OK");
            return true;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Post-restore connection pool validation failed", e);
            return false;
        }
    }
}
