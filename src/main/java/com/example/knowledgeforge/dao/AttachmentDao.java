package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.attachment.Attachment;
import com.example.knowledgeforge.domain.attachment.AttachmentType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class AttachmentDao {

    private static final Logger log = Logger.getLogger(AttachmentDao.class.getName());

    private final DataSource dataSource;

    public AttachmentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Attachment insert(Attachment a) {
        if (a.getCreatedAt() == null) a.setCreatedAt(Instant.now());
        String sql = """
                INSERT INTO attachment
                    (topic_id, original_name, stored_name, relative_path, content_type,
                     size_bytes, attachment_type, description, checksum_sha256, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, a.getTopicId());
            ps.setString(2, a.getOriginalName());
            ps.setString(3, a.getStoredName());
            ps.setString(4, a.getRelativePath());
            ps.setString(5, a.getContentType());
            ps.setLong(6, a.getSizeBytes());
            ps.setString(7, a.getAttachmentType().name());
            ps.setString(8, a.getDescription());
            ps.setString(9, a.getChecksumSha256());
            ps.setTimestamp(10, Timestamp.from(a.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getLong(1));
            }
            log.fine(() -> "Inserted attachment " + a.getId() + " for topic " + a.getTopicId());
            return a;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert attachment", e);
        }
    }

    public Optional<Attachment> findById(Long id) {
        String sql = "SELECT * FROM attachment WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query attachment by id", e);
        }
    }

    public List<Attachment> findAllByTopicId(UUID topicId) {
        String sql = "SELECT * FROM attachment WHERE topic_id = ? ORDER BY created_at DESC";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attachment> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list attachments by topic", e);
        }
    }

    /** Wszystkie relative_path w bazie — używane wyłącznie przez diagnostykę spójności dysk<->baza. */
    public List<String> findAllRelativePaths() {
        String sql = "SELECT relative_path FROM attachment";
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> result = new ArrayList<>();
            while (rs.next()) result.add(rs.getString(1));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list attachment paths", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM attachment WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.fine(() -> "Deleted attachment " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete attachment", e);
        }
    }

    private Attachment map(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setTopicId(rs.getObject("topic_id", UUID.class));
        a.setOriginalName(rs.getString("original_name"));
        a.setStoredName(rs.getString("stored_name"));
        a.setRelativePath(rs.getString("relative_path"));
        a.setContentType(rs.getString("content_type"));
        a.setSizeBytes(rs.getLong("size_bytes"));
        a.setAttachmentType(AttachmentType.valueOf(rs.getString("attachment_type")));
        a.setDescription(rs.getString("description"));
        a.setChecksumSha256(rs.getString("checksum_sha256"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return a;
    }
}
