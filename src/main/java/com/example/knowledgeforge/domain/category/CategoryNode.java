package com.example.knowledgeforge.domain.category;

import java.time.Instant;
import java.util.UUID;

public class CategoryNode {

    private UUID id;
    private Long userId;
    private UUID parentId;
    private String name;
    private Integer position = 0;
    private boolean root;
    private Instant createdAt;
    private Instant updatedAt;

    public CategoryNode() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public boolean isRoot() { return root; }
    public void setRoot(boolean root) { this.root = root; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
