package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.category.CategoryNode;

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

public class CategoryDao {

    private final DataSource dataSource;

    public CategoryDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<CategoryNode> findByIdAndUserId(UUID id, Long userId) {
        String sql = "SELECT * FROM category_node WHERE id = ? AND user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query category by id", e);
        }
    }

    public List<CategoryNode> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM category_node WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CategoryNode> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list categories", e);
        }
    }

    public Optional<CategoryNode> findByUserIdAndRootTrue(Long userId) {
        String sql = "SELECT * FROM category_node WHERE user_id = ? AND root = TRUE";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query root category", e);
        }
    }

    public boolean existsByParentId(UUID parentId) {
        String sql = "SELECT 1 FROM category_node WHERE parent_id = ? LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check for sub-categories", e);
        }
    }

    public long countByUserIdAndParentIdAndNameIgnoreCase(Long userId, UUID parentId, String name) {
        String sql = "SELECT COUNT(*) FROM category_node WHERE user_id = ? AND parent_id = ? AND lower(name) = lower(?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, parentId);
            ps.setString(3, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count sibling categories", e);
        }
    }

    public CategoryNode insert(CategoryNode node) {
        if (node.getId() == null) node.setId(UUID.randomUUID());
        Instant now = Instant.now();
        node.setCreatedAt(now);
        node.setUpdatedAt(now);

        String sql = """
                INSERT INTO category_node (id, user_id, parent_id, name, position, root, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, node.getId());
            ps.setLong(2, node.getUserId());
            ps.setObject(3, node.getParentId());
            ps.setString(4, node.getName());
            ps.setInt(5, node.getPosition());
            ps.setBoolean(6, node.isRoot());
            ps.setTimestamp(7, Timestamp.from(node.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.from(node.getUpdatedAt()));
            ps.executeUpdate();
            return node;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert category", e);
        }
    }

    public CategoryNode update(CategoryNode node) {
        node.setUpdatedAt(Instant.now());
        String sql = "UPDATE category_node SET name = ?, position = ?, updated_at = ? WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, node.getName());
            ps.setInt(2, node.getPosition());
            ps.setTimestamp(3, Timestamp.from(node.getUpdatedAt()));
            ps.setObject(4, node.getId());
            ps.executeUpdate();
            return node;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category", e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM category_node WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete category", e);
        }
    }

    private CategoryNode map(ResultSet rs) throws SQLException {
        CategoryNode node = new CategoryNode();
        node.setId(rs.getObject("id", UUID.class));
        node.setUserId(rs.getLong("user_id"));
        node.setParentId(rs.getObject("parent_id", UUID.class));
        node.setName(rs.getString("name"));
        node.setPosition(rs.getInt("position"));
        node.setRoot(rs.getBoolean("root"));
        node.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        node.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return node;
    }
}
