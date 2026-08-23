package com.example.knowledgeforge.support;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** Minimalne wiersze potrzebne do spełnienia kluczy obcych (user -> category -> topic) w testach DAO. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static long insertUser(DataSource ds, String username) throws Exception {
        String sql = "INSERT INTO users (username, email, active) VALUES (?, ?, true) RETURNING id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username + "@test.local");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static UUID insertCategory(DataSource ds, long userId, String name) throws Exception {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO category_node (id, user_id, parent_id, name, position, root, created_at, updated_at) "
                + "VALUES (?, ?, NULL, ?, 0, false, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setLong(2, userId);
            ps.setString(3, name);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.executeUpdate();
        }
        return id;
    }

    public static UUID insertTopic(DataSource ds, long userId, UUID categoryId, String title) throws Exception {
        return insertTopic(ds, userId, categoryId, title, Instant.now(), null, true, null);
    }

    /**
     * Wariant z pełną kontrolą nad polami istotnymi dla weryfikacji aktualności — używany przez
     * ActualityVerificationService/SchedulerTest do przygotowania rekordów po obu stronach
     * granicy okresu, z/bez wcześniejszej daty potwierdzenia, już oznaczonych jako nieaktualne itd.
     */
    public static UUID insertTopic(DataSource ds, long userId, UUID categoryId, String title, Instant createdAt,
                                    Instant lastVerificationOfActualityDate, boolean actualityVerified, String author) throws Exception {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO topic (id, user_id, category_id, title, author, detail_level, type, status, "
                + "created_at, updated_at, actuality_verified, last_verification_of_actuality_date) "
                + "VALUES (?, ?, ?, ?, ?, 'MEDIUM', 'NOTE', 'NEW', ?, ?, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setLong(2, userId);
            ps.setObject(3, categoryId);
            ps.setString(4, title);
            ps.setString(5, author);
            ps.setTimestamp(6, Timestamp.from(createdAt));
            ps.setTimestamp(7, Timestamp.from(createdAt));
            ps.setBoolean(8, actualityVerified);
            ps.setTimestamp(9, lastVerificationOfActualityDate == null ? null : Timestamp.from(lastVerificationOfActualityDate));
            ps.executeUpdate();
        }
        return id;
    }

    public static Integer topicVersion(DataSource ds, UUID topicId) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT version FROM topic WHERE id = ?")) {
            ps.setObject(1, topicId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public static Boolean topicActualityVerified(DataSource ds, UUID topicId) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT actuality_verified FROM topic WHERE id = ?")) {
            ps.setObject(1, topicId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    public static boolean topicExists(DataSource ds, UUID topicId) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM topic WHERE id = ?")) {
            ps.setObject(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void deleteTopic(DataSource ds, UUID topicId) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM topic WHERE id = ?")) {
            ps.setObject(1, topicId);
            ps.executeUpdate();
        }
    }
}
