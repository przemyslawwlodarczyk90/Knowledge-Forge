package com.example.knowledgeforge.domain.category.dto;

import com.example.knowledgeforge.domain.category.CategoryNode;

import java.util.UUID;

public class CategoryDto {

    private UUID id;
    private String name;
    private UUID parentId;
    private Integer position;
    private boolean root;

    public CategoryDto() {
    }

    public CategoryDto(UUID id, String name, UUID parentId, Integer position, boolean root) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.position = position;
        this.root = root;
    }

    public static CategoryDto from(CategoryNode node) {
        return new CategoryDto(
                node.getId(), node.getName(), node.getParentId(), node.getPosition(), node.isRoot()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public boolean isRoot() { return root; }
    public void setRoot(boolean root) { this.root = root; }
}
