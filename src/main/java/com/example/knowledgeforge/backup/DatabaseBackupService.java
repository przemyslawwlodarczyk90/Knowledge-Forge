package com.example.knowledgeforge.backup;

import com.example.knowledgeforge.config.AppConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backup CAŁEJ bazy PostgreSQL (użytkownicy, kategorie, tematy, notatki, metadane
 * załączników i relacje topic-attachment) przez oficjalne "pg_dump -Fc" — format custom,
 * binarny i skompresowany, czytany przez pg_restore. NIE kopiuje plików z
 * attachments.storage.path (to zadanie backupu dysku, nie tej klasy — zob. dokumentacja/BACKUP_AND_RESTORE.txt).
 *
 * Plik lądowuje najpierw jako "*.dump.part"; dopiero po exit code 0 z pg_dump i weryfikacji,
 * że coś realnie powstało, jest atomowo przenoszony na docelową nazwę. Retencja (usuwanie
 * .dump starszych niż backup.retention-days, z wyjątkiem najnowszego) uruchamia się dopiero
 * PO udanym utworzeniu nowego backupu.
 */
public class DatabaseBackupService {

    private static final Logger log = Logger.getLogger(DatabaseBackupService.class.getName());

    private static final DateTimeFormatter FILENAME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern DUMP_FILENAME = Pattern.compile(
            "^knowledge-forge_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.dump$");

    private final AppConfig config;
    private final PgConnectionInfo connInfo;
    private final MaintenanceGate gate;
    private final Path backupDir;

    private ScheduledExecutorService scheduler;

    public DatabaseBackupService(AppConfig config, MaintenanceGate gate) {
        this.config = config;
        this.gate = gate;
        this.connInfo = PgConnectionInfo.parse(config.dbUrl());
        this.backupDir = Path.of(config.backupDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.severe("Failed to create backup directory: " + backupDir + " -> " + e.getMessage());
            throw new UncheckedIOException("Failed to create backup directory: " + backupDir, e);
        }
        log.info(() -> "Backup directory: " + backupDir);
    }

    public Path backupDir() {
        return backupDir;
    }

    public static Pattern dumpFilenamePattern() {
        return DUMP_FILENAME;
    }

    // ── Harmonogram — codziennie o backup.schedule-hour:backup.schedule-minute, strefa lokalna ──

    public void start() {
        if (!config.backupEnabled()) {
            log.info("Automatic backup disabled (backup.enabled=false)");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "backup-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduleNext();
        log.info(() -> "Automatic daily backup scheduled for " + String.format("%02d:%02d", config.backupScheduleHour(), config.backupScheduleMinute())
                + " (" + ZoneId.systemDefault() + ")");
    }

    private void scheduleNext() {
        long delaySeconds = secondsUntilNextRun();
        scheduler.schedule(this::runScheduled, delaySeconds, TimeUnit.SECONDS);
    }

    private void runScheduled() {
        try {
            if (gate.isMaintenance()) {
                log.warning("Scheduled backup skipped: application is in maintenance mode (restore in progress)");
            } else {
                runBackupNow();
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Scheduled backup run failed unexpectedly", e);
        } finally {
            // Zawsze planujemy kolejne uruchomienie na następny dzień — niepowodzenie dzisiejszego
            // backupu nie może zablokować backupów w przyszłości. ScheduledExecutorService,
            // nie pętla ze sleep(): kolejne wywołanie liczy czas od nowa (odporne na DST).
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduleNext();
            }
        }
    }

    long secondsUntilNextRun() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = now.withHour(config.backupScheduleHour())
                .withMinute(config.backupScheduleMinute())
                .withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return java.time.Duration.between(now, next).getSeconds();
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ── Backup na żądanie (harmonogram i DatabaseRestoreService#emergencyBackup wołają to samo) ──

    public record BackupResult(boolean success, String fileName, String message) {
    }

    public BackupResult runBackupNow() {
        if (!gate.tryAcquireOperation()) {
            log.warning("Backup skipped: another backup or restore is already in progress");
            return new BackupResult(false, null, "Another backup or restore is already in progress");
        }
        try {
            return doBackup();
        } finally {
            gate.releaseOperation();
        }
    }

    private BackupResult doBackup() {
        String fileName = "knowledge-forge_" + FILENAME_FMT.format(LocalDateTime.now()) + ".dump";
        Path finalPath = backupDir.resolve(fileName);
        Path tempPath = backupDir.resolve(fileName + ".part");

        List<String> command = List.of(
                config.backupPgDumpPath(),
                "-h", connInfo.host(),
                "-p", String.valueOf(connInfo.port()),
                "-U", config.dbUsername(),
                "-d", connInfo.database(),
                "-Fc",
                "-f", tempPath.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", config.dbPassword());

        try {
            ProcessRunner.Outcome outcome = ProcessRunner.run(pb);
            String stderr = ProcessRunner.scrub(outcome.stderr(), config.dbPassword());

            if (outcome.exitCode() != 0) {
                deleteQuietly(tempPath);
                log.severe("pg_dump failed (exit=" + outcome.exitCode() + "): " + stderr);
                return new BackupResult(false, null, "pg_dump exited with code " + outcome.exitCode());
            }
            if (!Files.exists(tempPath) || Files.size(tempPath) == 0) {
                deleteQuietly(tempPath);
                log.severe("pg_dump reported success but produced no/empty output file: " + tempPath);
                return new BackupResult(false, null, "pg_dump produced no output");
            }
            if (!stderr.isBlank()) {
                log.fine(() -> "pg_dump stderr (non-fatal): " + stderr);
            }

            Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE);
            long size = Files.size(finalPath);
            log.info(() -> "Backup created: " + fileName + " (" + size + " bytes)");

            applyRetention();
            return new BackupResult(true, fileName, "OK");
        } catch (IOException e) {
            deleteQuietly(tempPath);
            log.log(Level.SEVERE, "pg_dump execution failed", e);
            return new BackupResult(false, null, "pg_dump execution failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deleteQuietly(tempPath);
            return new BackupResult(false, null, "pg_dump execution interrupted");
        }
    }

    /**
     * Usuwa kompletne pliki .dump starsze niż backup.retention-days — NIGDY najnowszego,
     * NIGDY plików spoza backupDir, NIGDY plików o nazwie nie pasującej do ścisłego wzorca
     * (a więc też nigdy ".part" — te w ogóle nie pasują do DUMP_FILENAME).
     * Widoczność pakietowa celowo (nie prywatna) — pozwala testom jednostkowym wywołać ją
     * bezpośrednio na spreparowanych plikach, bez uruchamiania prawdziwego pg_dump.
     */
    void applyRetention() {
        List<DatedDump> dumps = listValidDumps();
        if (dumps.isEmpty()) return;

        dumps.sort(Comparator.comparing(DatedDump::timestamp).reversed());
        DatedDump newest = dumps.get(0); // nigdy nie usuwany, niezależnie od wieku

        ZonedDateTime cutoff = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(config.backupRetentionDays());
        for (int i = 1; i < dumps.size(); i++) {
            DatedDump d = dumps.get(i);
            if (d.path.equals(newest.path)) continue;
            if (d.timestamp.isBefore(cutoff)) {
                try {
                    Files.delete(d.path);
                    log.info(() -> "Retention: removed old backup " + d.path.getFileName());
                } catch (IOException e) {
                    log.warning(() -> "Retention: failed to delete " + d.path.getFileName() + ": " + e.getMessage());
                }
            }
        }
    }

    /** Wyłącznie nazwa pliku + data + rozmiar — bez pełnej ścieżki systemowej (zob. AdminBackupServlet). */
    public record BackupFileDto(String fileName, Instant createdAt, long sizeBytes) {
    }

    public List<BackupFileDto> listBackups() {
        List<DatedDump> dumps = listValidDumps();
        dumps.sort(Comparator.comparing(DatedDump::timestamp).reversed());
        List<BackupFileDto> result = new ArrayList<>();
        for (DatedDump d : dumps) {
            try {
                result.add(new BackupFileDto(d.path.getFileName().toString(), d.timestamp.toInstant(), Files.size(d.path)));
            } catch (IOException e) {
                log.warning(() -> "Failed to stat backup file " + d.path.getFileName() + ": " + e.getMessage());
            }
        }
        return result;
    }

    private record DatedDump(Path path, ZonedDateTime timestamp) {
    }

    private List<DatedDump> listValidDumps() {
        List<DatedDump> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                Matcher m = DUMP_FILENAME.matcher(p.getFileName().toString());
                if (!m.matches()) continue; // wzorzec wyklucza *.dump.part i wszystko obce
                try {
                    LocalDateTime ts = LocalDateTime.parse(m.group(1), FILENAME_FMT);
                    result.add(new DatedDump(p, ts.atZone(ZoneId.systemDefault())));
                } catch (Exception ignored) {
                    // nazwa "prawie" pasująca, ale niesparsowalna data -> traktujemy jak obcy plik, nie ruszamy
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to list backup directory for retention", e);
        }
        return result;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warning(() -> "Failed to clean up temp backup file " + path + ": " + e.getMessage());
        }
    }
}
