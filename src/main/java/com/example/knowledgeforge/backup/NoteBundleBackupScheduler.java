package com.example.knowledgeforge.backup;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.example.knowledgeforge.config.AppConfig;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Odpowiedzialny WYŁĄCZNIE za cykliczne uruchamianie {@link NoteBundleBackupService#runBackupNow()}
 * zgodnie z harmonogramem cron z config.properties — sam mechanizm backupu żyje w serwisie, nie
 * tutaj. Wzorzec identyczny jak {@code actuality.ActualityVerificationScheduler} /
 * {@code backup.DatabaseBackupService}: pojedynczy, dedykowany wątek-demon, {@code schedule(...)}
 * liczące czas do KOLEJNEGO uruchomienia na nowo za każdym razem (nie stały interwał) — odporne na
 * przesunięcia czasu (DST) i na to, że pojedynczy przebieg mógł potrwać dłużej niż zwykle.
 *
 * Konfiguracja (cron, strefa) jest parsowana i WALIDOWANA w konstruktorze — BEZWARUNKOWO,
 * niezależnie od note-bundle-backup.enabled — więc błędna wartość zawsze przerywa start aplikacji
 * jawnym wyjątkiem (Main tworzy tę instancję zawsze; dopiero {@link #start()} sprawdza `enabled`
 * i decyduje, czy faktycznie uruchomić harmonogram).
 */
public class NoteBundleBackupScheduler {

    private static final Logger log = Logger.getLogger(NoteBundleBackupScheduler.class.getName());

    private final AppConfig config;
    private final NoteBundleBackupService service;
    private final ExecutionTime executionTime;

    private ScheduledExecutorService scheduler;

    public NoteBundleBackupScheduler(AppConfig config, NoteBundleBackupService service) {
        this.config = config;
        this.service = service;
        ZoneId zone = config.noteBundleBackupZoneId();
        Cron cron = parseAndValidateCron(config.noteBundleBackupCron());
        this.executionTime = ExecutionTime.forCron(cron);
        log.fine(() -> "Note bundle backup config OK — zone=" + zone + " cron='" + config.noteBundleBackupCron() + "'");
    }

    private static Cron parseAndValidateCron(String expression) {
        try {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
            Cron cron = parser.parse(expression);
            cron.validate();
            return cron;
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Invalid note-bundle-backup.cron '" + expression + "' — expected a 5-field UNIX cron "
                            + "expression (minute hour day-of-month month day-of-week), e.g. '30 17 * * *' "
                            + "(every day at 17:30)", e);
        }
    }

    public void start() {
        // Osierocony .part (przerwany proces w trakcie poprzedniego zapisu) sprzątamy zawsze,
        // niezależnie od `enabled` — zob. klasowy komentarz NoteBundleBackupService.
        service.cleanupOrphanedPartOnStartup();

        if (!config.noteBundleBackupEnabled()) {
            log.info("Note bundle backup scheduler disabled (note-bundle-backup.enabled=false)");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "note-bundle-backup-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduleNext();
        log.info(() -> "Note bundle backup scheduled: cron='" + config.noteBundleBackupCron()
                + "' zone=" + config.noteBundleBackupZoneId() + " directory=" + service.backupDir());
    }

    private void scheduleNext() {
        ZoneId zone = config.noteBundleBackupZoneId();
        ZonedDateTime now = ZonedDateTime.now(zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(now);
        if (next.isEmpty()) {
            log.warning("Note bundle backup cron has no next execution — scheduler stopped");
            return;
        }
        long delayMillis = Duration.between(now, next.get()).toMillis();
        log.fine(() -> "Next note bundle backup run at " + next.get() + " (in " + delayMillis + " ms)");
        scheduler.schedule(this::runScheduled, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void runScheduled() {
        try {
            log.info("Note bundle backup run started");
            NoteBundleBackupService.BackupResult result = service.runBackupNow();
            if (result.success()) {
                log.info(() -> "Note bundle backup run finished — " + result.documentCount() + " document(s), "
                        + result.corruptedCount() + " corrupted");
            } else {
                log.warning(() -> "Note bundle backup run did not produce a new backup: " + result.message());
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Note bundle backup run failed unexpectedly", e);
        } finally {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduleNext();
            }
        }
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
}
