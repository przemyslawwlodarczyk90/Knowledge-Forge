package com.example.knowledgeforge.actuality;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.example.knowledgeforge.config.AppConfig;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Odpowiedzialny WYŁĄCZNIE za cykliczne uruchamianie {@link ActualityVerificationService#runVerificationSweep()}
 * zgodnie z harmonogramem cron z config.properties (zob. ACTUALITY_VERIFICATION.txt) — sama
 * reguła "kiedy temat traci aktualność" żyje w serwisie, nie tutaj. Wzorzec identyczny jak
 * {@code backup.DatabaseBackupService}: pojedynczy, dedykowany wątek-demon, `schedule(...)`
 * liczące czas do KOLEJNEGO uruchomienia na nowo za każdym razem (nie stały `scheduleAtFixedRate`)
 * — odporne na przesunięcia czasu (DST) i na to, że pojedynczy przebieg mógł potrwać dłużej niż
 * "zwykle".
 *
 * Konfiguracja (okres, cron, strefa) jest parsowana i WALIDOWANA w konstruktorze —
 * BEZWARUNKOWO, niezależnie od actuality.verification.enabled — więc błędna wartość zawsze
 * przerywa start aplikacji jawnym wyjątkiem (Main tworzy tę instancję zawsze; dopiero
 * {@link #start()} sprawdza `enabled` i decyduje, czy faktycznie uruchomić harmonogram).
 */
public class ActualityVerificationScheduler {

    private static final Logger log = Logger.getLogger(ActualityVerificationScheduler.class.getName());

    private final AppConfig config;
    private final ActualityVerificationService service;
    private final ExecutionTime executionTime;

    private ScheduledExecutorService scheduler;
    /** Chroni przed równoległym uruchomieniem drugiego przebiegu — patrz klasowy komentarz. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ActualityVerificationScheduler(AppConfig config, ActualityVerificationService service) {
        this.config = config;
        this.service = service;
        // Walidacja BEZWARUNKOWA — patrz javadoc klasy. Każda z tych trzech linii rzuca jawny,
        // zrozumiały wyjątek, jeśli odpowiadająca wartość w config.properties jest błędna.
        Period period = config.actualityVerificationPeriod();
        ZoneId zone = config.actualityVerificationZoneId();
        Cron cron = parseAndValidateCron(config.actualityVerificationCron());
        this.executionTime = ExecutionTime.forCron(cron);
        log.fine(() -> "Actuality verification config OK — period=" + period + " zone=" + zone
                + " cron='" + config.actualityVerificationCron() + "'");
    }

    private static Cron parseAndValidateCron(String expression) {
        try {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
            Cron cron = parser.parse(expression);
            cron.validate();
            return cron;
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Invalid actuality.verification.cron '" + expression + "' — expected a 5-field UNIX cron "
                            + "expression (minute hour day-of-month month day-of-week), e.g. '0 17 * * 5' "
                            + "(every Friday at 17:00)", e);
        }
    }

    public void start() {
        if (!config.actualityVerificationEnabled()) {
            log.info("Actuality verification scheduler disabled (actuality.verification.enabled=false)");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "actuality-verification-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduleNext();
        log.info(() -> "Actuality verification scheduled: cron='" + config.actualityVerificationCron()
                + "' zone=" + config.actualityVerificationZoneId() + " period=" + config.actualityVerificationPeriod());
    }

    private void scheduleNext() {
        ZoneId zone = config.actualityVerificationZoneId();
        ZonedDateTime now = ZonedDateTime.now(zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(now);
        if (next.isEmpty()) {
            log.warning("Actuality verification cron has no next execution — scheduler stopped");
            return;
        }
        long delayMillis = Duration.between(now, next.get()).toMillis();
        log.fine(() -> "Next actuality verification run at " + next.get() + " (in " + delayMillis + " ms)");
        scheduler.schedule(this::runScheduled, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void runScheduled() {
        try {
            // Redundantne z tym, że to jednowątkowy executor (dwa zadania na nim i tak nigdy nie
            // biegną równolegle) — ale jawne, żeby zachowanie nie zależało od implementacji
            // executora i było odporne na ewentualny przyszły ręczny trigger.
            if (!running.compareAndSet(false, true)) {
                log.warning("Actuality verification run skipped: previous run is still in progress");
                return;
            }
            log.info("Actuality verification run started");
            try {
                int count = service.runVerificationSweep();
                log.info(() -> "Actuality verification run finished — marked " + count + " topic(s) as unverified");
            } finally {
                running.set(false);
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Actuality verification run failed unexpectedly", e);
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
