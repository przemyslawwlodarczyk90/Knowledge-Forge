package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.TopicType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TopicDao {

    private static final Logger log = Logger.getLogger(TopicDao.class.getName());

    private final DataSource dataSource;

    public TopicDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Topic> findByIdAndUserId(UUID id, Long userId) {
        String sql = "SELECT * FROM topic WHERE id = ? AND user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query topic by id", e);
        }
    }

    /** Wszystkie tematy/wpisy użytkownika, niezależnie od kategorii — używane przez wyszukiwarkę. */
    public List<Topic> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM topic WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all topics by user", e);
        }
    }

    public List<Topic> findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, UUID categoryId) {
        String sql = "SELECT * FROM topic WHERE user_id = ? AND category_id = ? ORDER BY created_at DESC";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list topics by category", e);
        }
    }

    public boolean existsByUserIdAndCategoryId(Long userId, UUID categoryId) {
        String sql = "SELECT 1 FROM topic WHERE user_id = ? AND category_id = ? LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check topics in category", e);
        }
    }

    public Topic insert(Topic topic) {
        if (topic.getId() == null) topic.setId(UUID.randomUUID());
        Instant now = Instant.now();
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        if (topic.getStatus() == null) topic.setStatus(TopicStatus.NEW);

        String sql = """
                INSERT INTO topic (id, user_id, category_id, title, short_prompt, author, detail_level, type, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, topic.getId());
            ps.setLong(2, topic.getUserId());
            ps.setObject(3, topic.getCategoryId());
            ps.setString(4, topic.getTitle());
            ps.setString(5, topic.getShortPrompt());
            ps.setString(6, topic.getAuthor());
            ps.setString(7, topic.getDetailLevel().name());
            ps.setString(8, topic.getType().name());
            ps.setString(9, topic.getStatus().name());
            ps.setTimestamp(10, Timestamp.from(topic.getCreatedAt()));
            ps.setTimestamp(11, Timestamp.from(topic.getUpdatedAt()));
            ps.executeUpdate();
            log.fine(() -> "Inserted topic " + topic.getId());
            return topic;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to insert topic " + topic.getId(), e);
            throw new RuntimeException("Failed to insert topic", e);
        }
    }

    public Topic update(Topic topic) {
        topic.setUpdatedAt(Instant.now());
        String sql = """
                UPDATE topic SET title = ?, short_prompt = ?, author = ?, detail_level = ?, type = ?, status = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, topic.getTitle());
            ps.setString(2, topic.getShortPrompt());
            ps.setString(3, topic.getAuthor());
            ps.setString(4, topic.getDetailLevel().name());
            ps.setString(5, topic.getType().name());
            ps.setString(6, topic.getStatus().name());
            ps.setTimestamp(7, Timestamp.from(topic.getUpdatedAt()));
            ps.setObject(8, topic.getId());
            ps.executeUpdate();
            log.fine(() -> "Updated topic " + topic.getId());
            return topic;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to update topic " + topic.getId(), e);
            throw new RuntimeException("Failed to update topic", e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM topic WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete topic", e);
        }
    }

    private Topic map(ResultSet rs) throws SQLException {
        Topic topic = new Topic();
        topic.setId(rs.getObject("id", UUID.class));
        topic.setUserId(rs.getLong("user_id"));
        topic.setCategoryId(rs.getObject("category_id", UUID.class));
        topic.setTitle(rs.getString("title"));
        topic.setShortPrompt(rs.getString("short_prompt"));
        topic.setAuthor(rs.getString("author"));
        topic.setDetailLevel(DetailLevel.valueOf(rs.getString("detail_level")));
        topic.setType(TopicType.valueOf(rs.getString("type")));
        topic.setStatus(TopicStatus.valueOf(rs.getString("status")));
        topic.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        topic.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return topic;
    }
}
