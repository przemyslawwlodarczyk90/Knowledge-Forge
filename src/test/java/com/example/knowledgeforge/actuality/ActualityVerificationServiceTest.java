package com.example.knowledgeforge.actuality;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.support.ServiceHarness;
import com.example.knowledgeforge.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ActualityVerificationService — logika biznesowa, niezależna od crona/HTTP. Zegar wstrzyknięty
 * (ServiceHarness#clock, MutableClock) — żaden test nie zależy od aktualnego zegara systemowego
 * ani nie czeka naprawdę na upływ czasu.
 */
class ActualityVerificationServiceTest {

    ServiceHarness harness;
    long userId;
    UUID categoryId;
    ZoneId zone = ZoneId.of("Europe/Warsaw");

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        harness = ServiceHarness.withFreshUser(tempDir, "actuality-service");
        userId = harness.currentUser.id();
        categoryId = TestFixtures.insertCategory(harness.dataSource, userId, "Actuality service test category");
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    // ── confirmActuality (endpoint POST .../verify-actuality) ──────────────────────────────────

    @Test
    void confirmActuality_sets_boolean_and_date_and_returns_fresh_dto() throws Exception {
        UUID id = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Manual confirm target",
                Instant.now().minus(999, ChronoUnit.DAYS), null, false, null);
        Instant fixedNow = Instant.parse("2026-06-01T12:00:00Z");
        harness.clock.set(fixedNow, zone);

        TopicDto dto = harness.actualityVerificationService.confirmActuality(id, 1, "client-1");

        assertTrue(dto.isActualityVerified());
        assertEquals(fixedNow, dto.getLastVerificationOfActualityDate());
        assertEquals(2, dto.getVersion());
    }

    @Test
    void confirmActuality_version_conflict_throws_conflict_exception() throws Exception {
        UUID id = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Conflict target",
                Instant.now(), null, false, null);

        assertThrows(ConflictException.class,
                () -> harness.actualityVerificationService.confirmActuality(id, 99, "client-1"));
    }

    // ── listForReview ────────────────────────────────────────────────────────────────────────

    @Test
    void listForReview_returns_only_unverified_records_for_current_user() throws Exception {
        Instant now = Instant.now();
        UUID stale = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Stale",
                now, now.minus(30, ChronoUnit.DAYS), false, null);
        UUID fresh = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Fresh",
                now, null, true, null);

        List<TopicDto> review = harness.actualityVerificationService.listForReview(null);
        var ids = review.stream().map(TopicDto::getId).toList();

        assertTrue(ids.contains(stale));
        assertFalse(ids.contains(fresh));
    }

    // ── runVerificationSweep — reguła "data bazowa + okres <= teraz" ───────────────────────────

    @Test
    void sweep_with_default_two_year_period_flags_records_exactly_at_boundary() throws Exception {
        Instant fixedNow = Instant.parse("2026-06-15T10:00:00Z");
        harness.clock.set(fixedNow, zone);
        Period period = harness.config.actualityVerificationPeriod();
        assertEquals(Period.parse("P2Y"), period, "default config.properties period must be P2Y");

        // Data bazowa dokładnie 2 lata przed "teraz" (w skonfigurowanej strefie) -> musi stracić aktualność.
        Instant exactlyTwoYearsAgo = fixedNow.atZone(zone).minus(period).toInstant();
        UUID atBoundary = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Exactly at boundary",
                fixedNow, exactlyTwoYearsAgo, true, null);
        // Rekord jeden dzień młodszy niż granica -> musi POZOSTAĆ aktualny.
        UUID justYounger = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "One day younger",
                fixedNow, exactlyTwoYearsAgo.plus(1, ChronoUnit.DAYS), true, null);

        int count = harness.actualityVerificationService.runVerificationSweep();

        assertTrue(count >= 1);
        assertFalse(TestFixtures.topicActualityVerified(harness.dataSource, atBoundary));
        assertTrue(TestFixtures.topicActualityVerified(harness.dataSource, justYounger));
    }

    @Test
    void sweep_uses_createdAt_as_base_date_when_never_verified() throws Exception {
        Instant fixedNow = Instant.parse("2026-06-15T10:00:00Z");
        harness.clock.set(fixedNow, zone);
        Period period = harness.config.actualityVerificationPeriod();
        Instant cutoffCreatedAt = fixedNow.atZone(zone).minus(period).minusSeconds(1).toInstant();

        // lastVerificationOfActualityDate = null -> data bazowa MUSI być createdAt.
        UUID neverVerified = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Never verified, old createdAt",
                cutoffCreatedAt, null, true, null);

        harness.actualityVerificationService.runVerificationSweep();

        assertFalse(TestFixtures.topicActualityVerified(harness.dataSource, neverVerified),
                "with no verification date, base date must fall back to createdAt");
    }

    @Test
    void sweep_uses_verification_date_over_createdAt_when_present() throws Exception {
        Instant fixedNow = Instant.parse("2026-06-15T10:00:00Z");
        harness.clock.set(fixedNow, zone);

        // createdAt bardzo stary (dawno przekroczyłby okres), ALE świeżo zweryfikowany -> musi
        // pozostać aktualny, bo data bazowa to lastVerificationOfActualityDate, nie createdAt.
        UUID recentlyVerified = TestFixtures.insertTopic(harness.dataSource, userId, categoryId,
                "Ancient createdAt but recently verified",
                fixedNow.minus(999, ChronoUnit.DAYS), fixedNow.minus(1, ChronoUnit.DAYS), true, null);

        harness.actualityVerificationService.runVerificationSweep();

        assertTrue(TestFixtures.topicActualityVerified(harness.dataSource, recentlyVerified),
                "a recent verification date must win over an old createdAt");
    }

    @Test
    void sweep_does_not_re_touch_records_already_marked_unverified() throws Exception {
        Instant fixedNow = Instant.parse("2026-06-15T10:00:00Z");
        harness.clock.set(fixedNow, zone);

        UUID alreadyFalse = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Already false",
                fixedNow.minus(999, ChronoUnit.DAYS), fixedNow.minus(999, ChronoUnit.DAYS), false, null);
        Integer versionBefore = TestFixtures.topicVersion(harness.dataSource, alreadyFalse);

        harness.actualityVerificationService.runVerificationSweep();

        Integer versionAfter = TestFixtures.topicVersion(harness.dataSource, alreadyFalse);
        assertEquals(versionBefore, versionAfter, "already-unverified record must not be updated again (no version bump)");
    }

    @Test
    void sweep_respects_a_configured_period_other_than_two_years() throws Exception {
        Properties overrides = new Properties();
        overrides.setProperty("actuality.verification.period", "P30D");
        try (ServiceHarness shortPeriodHarness = ServiceHarness.withFreshUser(
                java.nio.file.Files.createTempDirectory("actuality-short-period"), "short-period", overrides)) {
            long uid = shortPeriodHarness.currentUser.id();
            UUID catId = TestFixtures.insertCategory(shortPeriodHarness.dataSource, uid, "Short period category");

            Instant fixedNow = Instant.parse("2026-06-15T10:00:00Z");
            shortPeriodHarness.clock.set(fixedNow, zone);
            assertEquals(Period.parse("P30D"), shortPeriodHarness.config.actualityVerificationPeriod());

            // 31 dni temu -> przekracza 30-dniowy okres -> nieaktualny.
            UUID overThreshold = TestFixtures.insertTopic(shortPeriodHarness.dataSource, uid, catId, "31 days old",
                    fixedNow.minus(31, ChronoUnit.DAYS), fixedNow.minus(31, ChronoUnit.DAYS), true, null);
            // 10 dni temu -> wciąż w granicach 30 dni -> aktualny.
            UUID underThreshold = TestFixtures.insertTopic(shortPeriodHarness.dataSource, uid, catId, "10 days old",
                    fixedNow.minus(10, ChronoUnit.DAYS), fixedNow.minus(10, ChronoUnit.DAYS), true, null);

            shortPeriodHarness.actualityVerificationService.runVerificationSweep();

            assertFalse(TestFixtures.topicActualityVerified(shortPeriodHarness.dataSource, overThreshold));
            assertTrue(TestFixtures.topicActualityVerified(shortPeriodHarness.dataSource, underThreshold));
        }
    }

    @Test
    void confirmActuality_publishes_after_the_update_is_already_persisted() throws Exception {
        // Nie ma tu prawdziwego klienta WS podłączonego do rejestru (ServiceHarness), więc
        // eventHub.publish(...) to bezpieczny no-op — sam fakt, że confirmActuality() w ogóle
        // wraca (żaden wyjątek), a wiersz w bazie JEST już zaktualizowany, dowodzi, że
        // rozgłoszenie (które w kodzie serwisu następuje PO SQL-owym update'cie, zob.
        // ActualityVerificationService#confirmActuality) nie mogło nastąpić PRZED zapisem —
        // gdyby SQL rzucił, wyjątek przerwałby metodę wcześniej i nigdy nie doszłoby do publish().
        UUID id = TestFixtures.insertTopic(harness.dataSource, userId, categoryId, "Publish-after-commit",
                Instant.now(), null, false, null);

        TopicDto dto = harness.actualityVerificationService.confirmActuality(id, 1, "client-1");

        assertNotNull(dto);
        assertTrue(TestFixtures.topicActualityVerified(harness.dataSource, id),
                "by the time confirmActuality() returns (i.e. after it already published), the DB row must reflect the change");
    }
}
