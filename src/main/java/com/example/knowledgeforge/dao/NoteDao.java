package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.note.Note;

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

/**
 * Baza trzyma tylko ścieżkę do pliku treści (content_path) — sama zawartość
 * .kfdoc leży na dysku, zob. storage.NoteFileStorage.
 */
public class NoteDao {

    private final DataSource dataSource;

    public NoteDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Note> findByTopicId(UUID topicId) {
        String sql = "SELECT * FROM note WHERE topic_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query note by topic", e);
        }
    }

    public Optional<Note> findByTopicIdAndUserId(UUID topicId, Long userId) {
        String sql = "SELECT * FROM note WHERE topic_id = ? AND user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, topicId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query note by topic and user", e);
        }
    }

    /** Wszystkie notatki użytkownika — używane przez wyszukiwarkę (przeszukuje treść). */
    public List<Note> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM note WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Note> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list notes by user", e);
        }
    }

    public Note insert(Note note) {
        if (note.getId() == null) note.setId(UUID.randomUUID());
        Instant now = Instant.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        if (note.getVersion() == null) note.setVersion(1);

        String sql = """
                INSERT INTO note (id, user_id, topic_id, content_path, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, note.getId());
            ps.setLong(2, note.getUserId());
            ps.setObject(3, note.getTopicId());
            ps.setString(4, note.getContentPath());
            ps.setInt(5, note.getVersion());
            ps.setTimestamp(6, Timestamp.from(note.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(note.getUpdatedAt()));
            ps.executeUpdate();
            return note;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert note", e);
        }
    }

    public Note update(Note note) {
        note.setUpdatedAt(Instant.now());
        String sql = "UPDATE note SET content_path = ?, version = ?, updated_at = ? WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, note.getContentPath());
            ps.setInt(2, note.getVersion());
            ps.setTimestamp(3, Timestamp.from(note.getUpdatedAt()));
            ps.setObject(4, note.getId());
            ps.executeUpdate();
            return note;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update note", e);
        }
    }

    private Note map(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getObject("id", UUID.class));
        note.setUserId(rs.getLong("user_id"));
        note.setTopicId(rs.getObject("topic_id", UUID.class));
        note.setContentPath(rs.getString("content_path"));
        note.setVersion(rs.getInt("version"));
        note.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        note.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return note;
    }
}
