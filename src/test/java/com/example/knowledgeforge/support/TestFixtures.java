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
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO topic (id, user_id, category_id, title, detail_level, type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'MEDIUM', 'NOTE', 'NEW', ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setObject(1, id);
            ps.setLong(2, userId);
            ps.setObject(3, categoryId);
            ps.setString(4, title);
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            ps.executeUpdate();
        }
        return id;
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
