package com.example.knowledgeforge.actuality;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.example.knowledgeforge.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ActualityVerificationScheduler — walidacja konfiguracji (musi rzucać w konstruktorze,
 * BEZWARUNKOWO, niezależnie od actuality.verification.enabled) i poprawność wyliczenia
 * następnego uruchomienia z crona + strefy. Nie uruchamia prawdziwego harmonogramu (żaden test
 * tu nie woła #start()) — czysto synchroniczne, bez wątków/oczekiwania.
 */
class ActualityVerificationSchedulerTest {

    private static AppConfig configWith(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return AppConfig.fromProperties(props);
    }

    private static AppConfig configWith(Properties props) {
        return AppConfig.fromProperties(props);
    }

    @Test
    void invalid_period_fails_construction_even_when_disabled() {
        Properties props = new Properties();
        props.setProperty("actuality.verification.enabled", "false"); // celowo wyłączone
        props.setProperty("actuality.verification.period", "not-a-period");
        AppConfig config = configWith(props);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ActualityVerificationScheduler(config, null));
        assertTrue(ex.getMessage().contains("actuality.verification.period"));
    }

    @Test
    void invalid_cron_fails_construction() {
        AppConfig config = configWith("actuality.verification.cron", "not a cron expression");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ActualityVerificationScheduler(config, null));
        assertTrue(ex.getMessage().contains("actuality.verification.cron"));
    }

    @Test
    void invalid_zone_fails_construction() {
        AppConfig config = configWith("actuality.verification.zone-id", "Not/AZone");
        assertThrows(IllegalStateException.class, () -> new ActualityVerificationScheduler(config, null));
    }

    @Test
    void valid_config_constructs_without_throwing() {
        AppConfig config = AppConfig.fromProperties(new Properties()); // wszystko domyślne z config.properties-equivalent defaults
        new ActualityVerificationScheduler(config, null); // nie rzuca
    }

    @Test
    void next_execution_for_friday_17_00_cron_lands_on_the_correct_friday() {
        // "0 17 * * 5" = każdy piątek o 17:00, w skonfigurowanej strefie.
        AppConfig config = AppConfig.fromProperties(new Properties());
        assertEquals("0 17 * * 5", config.actualityVerificationCron());
        ZoneId zone = ZoneId.of("Europe/Warsaw");

        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        var cron = parser.parse(config.actualityVerificationCron());
        var executionTime = ExecutionTime.forCron(cron);

        // Poniedziałek 2026-06-15 10:00 (Europe/Warsaw) -> następny piątek to 2026-06-19.
        ZonedDateTime monday = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(monday);

        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2026, 6, 19, 17, 0, 0, 0, zone), next.get());
    }

    @Test
    void next_execution_after_fridays_run_time_rolls_over_to_the_following_friday() {
        AppConfig config = AppConfig.fromProperties(new Properties());
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        var cron = parser.parse(config.actualityVerificationCron());
        var executionTime = ExecutionTime.forCron(cron);

        // Piątek 2026-06-19 17:30 — PO zaplanowanej 17:00 tego samego dnia -> kolejne wykonanie
        // to następny piątek (2026-06-26), nie ten sam dzień ponownie.
        ZonedDateTime fridayEvening = ZonedDateTime.of(2026, 6, 19, 17, 30, 0, 0, zone);
        Optional<ZonedDateTime> next = executionTime.nextExecution(fridayEvening);

        assertTrue(next.isPresent());
        assertEquals(ZonedDateTime.of(2026, 6, 26, 17, 0, 0, 0, zone), next.get());
    }
}
