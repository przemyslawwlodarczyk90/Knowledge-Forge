package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.TopicType;
import com.example.knowledgeforge.support.TestDatabase;
import com.example.knowledgeforge.support.TestFixtures;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DAO/mapowanie mechanizmu weryfikacji aktualności — zob. dokumentacja/ACTUALITY_VERIFICATION.txt. Integracyjny,
 * łączy się z osobną bazą testową kf_test (TestDatabase), nigdy z bazą roboczą.
 */
class TopicDaoActualityTest {

    static HikariDataSource ds;
    static TopicDao dao;
    static long userId;
    static UUID categoryId;

    @BeforeAll
    static void setUpAll() throws Exception {
        ds = TestDatabase.create();
        dao = new TopicDao(ds);
        userId = TestFixtures.insertUser(ds, "actuality-dao-" + UUID.randomUUID());
        categoryId = TestFixtures.insertCategory(ds, userId, "Actuality DAO test category");
    }

    @AfterAll
    static void tearDownAll() {
        ds.close();
    }

    // ── 1. Migracja kolumn (schema.ensure już uruchomiony przez TestDatabase.create()) ────────

    @Test
    void migration_added_both_columns_with_correct_types() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT column_name, data_type, is_nullable, column_default FROM information_schema.columns "
                             + "WHERE table_name = 'topic' AND column_name IN ('actuality_verified', 'last_verification_of_actuality_date')")) {
            var found = new java.util.HashMap<String, String[]>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.put(rs.getString("column_name"),
                            new String[]{rs.getString("data_type"), rs.getString("is_nullable"), rs.getString("column_default")});
                }
            }
            assertTrue(found.containsKey("actuality_verified"));
            assertEquals("boolean", found.get("actuality_verified")[0]);
            assertEquals("NO", found.get("actuality_verified")[1], "actuality_verified must be NOT NULL");
            assertTrue(found.get("actuality_verified")[2] != null && found.get("actuality_verified")[2].contains("true"));

            assertTrue(found.containsKey("last_verification_of_actuality_date"));
            assertEquals("timestamp with time zone", found.get("last_verification_of_actuality_date")[0]);
            assertEquals("YES", found.get("last_verification_of_actuality_date")[1], "last_verification_of_actuality_date must allow NULL");
        }
    }

    @Test
    void migration_created_partial_index_for_unverified_lookup() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT indexdef FROM pg_indexes WHERE tablename = 'topic' AND indexname = 'idx_topic_actuality_unverified'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "expected idx_topic_actuality_unverified to exist");
                String def = rs.getString(1).toLowerCase();
                assertTrue(def.contains("actuality_verified = false"), "must be a partial index on actuality_verified = FALSE");
            }
        }
    }

    // ── 2. Nowy temat: actualityVerified=true, data null ───────────────────────────────────────

    @Test
    void newly_inserted_topic_defaults_to_verified_true_with_null_date() {
        Topic t = freshTopic();
        Topic saved = dao.insert(t);

        assertTrue(saved.isActualityVerified());
        assertNull(saved.getLastVerificationOfActualityDate());

        Topic reloaded = dao.findByIdAndUserId(saved.getId(), userId).orElseThrow();
        assertTrue(reloaded.isActualityVerified());
        assertNull(reloaded.getLastVerificationOfActualityDate());
    }

    // ── 3. Mapowanie pól DAO (round-trip insert -> read z ustawionymi polami aktualności) ──────

    @Test
    void dao_mapping_round_trips_actuality_fields_when_explicitly_set() throws Exception {
        Instant verifiedAt = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        UUID id = TestFixtures.insertTopic(ds, userId, categoryId, "Manually verified topic",
                Instant.now(), verifiedAt, true, "Jan");

        Topic reloaded = dao.findByIdAndUserId(id, userId).orElseThrow();
        assertTrue(reloaded.isActualityVerified());
        assertEquals(verifiedAt, reloaded.getLastVerificationOfActualityDate());
    }

    // ── confirmActuality (endpoint POST .../verify-actuality) ──────────────────────────────────

    @Test
    void confirmActuality_sets_verified_true_and_date_and_bumps_version() throws Exception {
        UUID id = TestFixtures.insertTopic(ds, userId, categoryId, "To confirm",
                Instant.now().minus(400, ChronoUnit.DAYS), null, false, null);
        Instant verifiedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        try (Connection con = ds.getConnection()) {
            int affected = dao.confirmActuality(con, id, 1, verifiedAt);
            assertEquals(1, affected);
        }

        Topic reloaded = dao.findByIdAndUserId(id, userId).orElseThrow();
        assertTrue(reloaded.isActualityVerified());
        assertEquals(verifiedAt, reloaded.getLastVerificationOfActualityDate());
        assertEquals(2, reloaded.getVersion());
    }

    @Test
    void confirmActuality_with_wrong_expectedVersion_affects_nothing() throws Exception {
        UUID id = TestFixtures.insertTopic(ds, userId, categoryId, "Version mismatch",
                Instant.now(), null, false, null);
        try (Connection con = ds.getConnection()) {
            int affected = dao.confirmActuality(con, id, 99, Instant.now());
            assertEquals(0, affected, "mismatched version must affect 0 rows, never throw at the DAO level");
        }
        Topic reloaded = dao.findByIdAndUserId(id, userId).orElseThrow();
        assertFalse(reloaded.isActualityVerified(), "unaffected row must remain untouched");
    }

    // ── markNoteSaved (NoteService#save side effect) ────────────────────────────────────────────

    @Test
    void markNoteSaved_sets_status_and_confirms_actuality_in_one_update() throws Exception {
        UUID id = TestFixtures.insertTopic(ds, userId, categoryId, "Note save target",
                Instant.now().minus(999, ChronoUnit.DAYS), null, false, null);
        Instant verifiedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        try (Connection con = ds.getConnection()) {
            int affected = dao.markNoteSaved(con, id, TopicStatus.NOTE_ADDED, 1, verifiedAt);
            assertEquals(1, affected);
        }

        Topic reloaded = dao.findByIdAndUserId(id, userId).orElseThrow();
        assertEquals(TopicStatus.NOTE_ADDED, reloaded.getStatus());
        assertTrue(reloaded.isActualityVerified());
        assertEquals(verifiedAt, reloaded.getLastVerificationOfActualityDate());
        assertEquals(2, reloaded.getVersion());
    }

    // ── markStaleAsUnverified (scheduler sweep) ─────────────────────────────────────────────────

    @Test
    void markStaleAsUnverified_flags_records_at_or_before_cutoff_only() throws Exception {
        Instant cutoff = Instant.now().truncatedTo(ChronoUnit.MICROS);

        UUID exactlyAtCutoff = TestFixtures.insertTopic(ds, userId, categoryId, "At cutoff",
                Instant.now(), cutoff, true, null);
        UUID beforeCutoff = TestFixtures.insertTopic(ds, userId, categoryId, "Before cutoff",
                Instant.now(), cutoff.minusSeconds(3600), true, null);
        UUID afterCutoff = TestFixtures.insertTopic(ds, userId, categoryId, "After cutoff (younger)",
                Instant.now(), cutoff.plusSeconds(3600), true, null);
        UUID alreadyFalse = TestFixtures.insertTopic(ds, userId, categoryId, "Already unverified",
                Instant.now(), cutoff.minusSeconds(3600), false, null);

        List<Topic> changed = dao.markStaleAsUnverified(cutoff);
        var changedIds = changed.stream().map(Topic::getId).toList();

        assertTrue(changedIds.contains(exactlyAtCutoff), "exactly at cutoff must become unverified (<=)");
        assertTrue(changedIds.contains(beforeCutoff));
        assertFalse(changedIds.contains(afterCutoff), "younger than cutoff must stay verified");
        assertFalse(changedIds.contains(alreadyFalse), "already-false record must not be touched again");

        // Version bumped for the ones we actually changed.
        Topic reloaded = dao.findByIdAndUserId(exactlyAtCutoff, userId).orElseThrow();
        assertFalse(reloaded.isActualityVerified());
        assertEquals(2, reloaded.getVersion());

        // Already-false record's version must be untouched (no re-update).
        Topic untouched = dao.findByIdAndUserId(alreadyFalse, userId).orElseThrow();
        assertEquals(1, untouched.getVersion());
    }

    @Test
    void markStaleAsUnverified_does_not_change_lastVerificationOfActualityDate() throws Exception {
        Instant cutoff = Instant.now();
        UUID id = TestFixtures.insertTopic(ds, userId, categoryId, "Date must stay null",
                Instant.now().minus(999, ChronoUnit.DAYS), null, true, null);

        dao.markStaleAsUnverified(cutoff);

        Topic reloaded = dao.findByIdAndUserId(id, userId).orElseThrow();
        assertFalse(reloaded.isActualityVerified());
        assertNull(reloaded.getLastVerificationOfActualityDate(), "sweep marks unverified WITHOUT touching the verification date");
    }

    // ── findForActualityReview (GET /api/topics/actuality-review) ──────────────────────────────

    @Test
    void findForActualityReview_returns_only_unverified_sorted_oldest_first_and_isolated_by_user() throws Exception {
        long otherUserId = TestFixtures.insertUser(ds, "other-user-" + UUID.randomUUID());
        UUID otherCategoryId = TestFixtures.insertCategory(ds, otherUserId, "Other user's category");

        Instant now = Instant.now();
        UUID oldest = TestFixtures.insertTopic(ds, userId, categoryId, "Oldest base date",
                now, now.minus(30, ChronoUnit.DAYS), false, "Ala");
        UUID middle = TestFixtures.insertTopic(ds, userId, categoryId, "Middle base date",
                now, now.minus(10, ChronoUnit.DAYS), false, "Bob");
        UUID verified = TestFixtures.insertTopic(ds, userId, categoryId, "Still verified",
                now, null, true, null); // must NOT appear — actuality_verified = true
        UUID otherUsers = TestFixtures.insertTopic(ds, otherUserId, otherCategoryId, "Belongs to someone else",
                now, now.minus(999, ChronoUnit.DAYS), false, null); // must NOT appear — different user

        List<Topic> review = dao.findForActualityReview(userId, null);
        var ids = review.stream().map(Topic::getId).toList();

        assertTrue(ids.contains(oldest));
        assertTrue(ids.contains(middle));
        assertFalse(ids.contains(verified), "actuality_verified = true records must be excluded");
        assertFalse(ids.contains(otherUsers), "records from another user must never leak in");
        assertTrue(ids.indexOf(oldest) < ids.indexOf(middle), "must be sorted oldest base date first");
    }

    @Test
    void findForActualityReview_filters_by_author_when_provided() throws Exception {
        Instant now = Instant.now();
        UUID byAla = TestFixtures.insertTopic(ds, userId, categoryId, "Ala's stale topic",
                now, now.minus(30, ChronoUnit.DAYS), false, "Ala-" + UUID.randomUUID());
        String authorTag = "Filter-" + UUID.randomUUID();
        UUID byTagged = TestFixtures.insertTopic(ds, userId, categoryId, "Tagged author's stale topic",
                now, now.minus(20, ChronoUnit.DAYS), false, authorTag);

        List<Topic> filtered = dao.findForActualityReview(userId, authorTag);
        var ids = filtered.stream().map(Topic::getId).toList();

        assertTrue(ids.contains(byTagged));
        assertFalse(ids.contains(byAla), "filtering by author must exclude other authors' stale topics");
    }

    private Topic freshTopic() {
        Topic t = new Topic();
        t.setUserId(userId);
        t.setCategoryId(categoryId);
        t.setTitle("Fresh topic " + UUID.randomUUID());
        t.setDetailLevel(DetailLevel.MEDIUM);
        t.setType(TopicType.NOTE);
        t.setStatus(TopicStatus.NEW);
        return t;
    }
}
