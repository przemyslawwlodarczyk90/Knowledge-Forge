package com.example.knowledgeforge.backup;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.support.TestAppConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NoteBundleBackupScheduler — walidacja konfiguracji (musi rzucać w konstruktorze, BEZWARUNKOWO,
 * niezależnie od note-bundle-backup.enabled) i poprawność wyliczenia następnego uruchomienia z
 * crona + strefy dla domyślnego "30 17 * * *" (codziennie 17:30). Nie uruchamia prawdziwego
 * harmonogramu (żaden test tu nie woła #start()) — czysto synchroniczne, bez wątków/oczekiwania.
 * Zob. też NoteBundleBackupServiceTest — sam mechanizm backupu.
 */
class NoteBundleBackupSchedulerTest {

    private static AppConfig configWith(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return AppConfig.fromProperties(props);
    }

    @Test
    void invalid_cron_fails_construction_even_when_disabled() {
        Properties props = new Properties();
        props.setProperty("note-bundle-backup.enabled", "false"); // celowo wyłączone
        props.setProperty("note-bundle-backup.cron", "not a cron expression");
        AppConfig config = AppConfig.fromProperties(props);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new NoteBundleBackupScheduler(config, null));
        assertTrue(ex.getMessage().contains("note-bundle-backup.cron"));
    }

    @Test
    void invalid_zone_fails_construction() {
        AppConfig config = configWith("note-bundle-backup.zone-id", "Not/AZone");
        assertThrows(IllegalStateException.class, () -> new NoteBundleBackupScheduler(config, null));
    }

    @Test
    void valid_config_constructs_without_throwing() {
        AppConfig config = AppConfig.fromProperties(new Properties()); // wszystko domyślne
        new NoteBundleBackupScheduler(config, null); // nie rzuca
    }

    @Test
    void next_execution_for_daily_17_30_cron_lands_the_same_day_when_run_before_that_time() {
        AppConfig config = AppConfig.fromProperties(new Properties());
        assertEquals("30 17 * * *", config.noteBundleBackupCron());
        ZoneId zone = ZoneId.of("Europe/Warsaw");

        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        var cron = parser.parse(config.noteBundleBackupCron());
        var executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime morning = ZonedDateTime.of(2026, 6, 15, 9, 0, 0, 0, zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(morning);

        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2026, 6, 15, 17, 30, 0, 0, zone), next.get());
    }

    @Test
    void next_execution_after_todays_run_time_rolls_over_to_tomorrow() {
        AppConfig config = AppConfig.fromProperties(new Properties());
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        var cron = parser.parse(config.noteBundleBackupCron());
        var executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime evening = ZonedDateTime.of(2026, 6, 15, 18, 0, 0, 0, zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(evening);

        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2026, 6, 16, 17, 30, 0, 0, zone), next.get());
    }

    @Test
    void custom_cron_from_properties_is_honored_instead_of_the_default() {
        AppConfig config = configWith("note-bundle-backup.cron", "0 9 * * 1"); // każdy poniedziałek 9:00
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        var cron = parser.parse(config.noteBundleBackupCron());
        var executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime tuesday = ZonedDateTime.of(2026, 6, 16, 10, 0, 0, 0, zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(tuesday);

        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2026, 6, 22, 9, 0, 0, 0, zone), next.get()); // następny poniedziałek
    }

    @Test
    void start_and_shutdown_do_not_throw_with_backup_disabled(@TempDir Path tmp) throws Exception {
        Path notesDir = Path.of(tmp.toString(), "notes");
        Path backupDir = Path.of(tmp.toString(), "note-bundle-backups");
        AppConfig config = TestAppConfigs.withNoteBundleBackup(notesDir, backupDir, "30 17 * * *", "Europe/Warsaw", false);
        NoteBundleBackupScheduler scheduler = new NoteBundleBackupScheduler(config, new NoteBundleBackupService(config));
        scheduler.start();   // disabled -> sprząta osierocony .part, ale NIE tworzy wątku harmonogramu
        scheduler.shutdown(); // no-op, scheduler nigdy nie powstał
    }
}
