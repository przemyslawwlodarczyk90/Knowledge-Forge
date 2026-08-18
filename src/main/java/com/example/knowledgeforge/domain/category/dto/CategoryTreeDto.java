package com.example.knowledgeforge.domain.category.dto;

import com.example.knowledgeforge.domain.category.CategoryNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CategoryTreeDto {

    private UUID id;
    private String name;
    private List<CategoryTreeDto> children;

    public CategoryTreeDto() {
    }

    public CategoryTreeDto(UUID id, String name, List<CategoryTreeDto> children) {
        this.id = id;
        this.name = name;
        this.children = children;
    }

    public static CategoryTreeDto fromRoot(CategoryNode root, Map<UUID, List<CategoryNode>> byParent) {
        List<CategoryNode> directChildren = byParent.getOrDefault(root.getId(), List.of());
        List<CategoryTreeDto> childDtos = directChildren.stream()
                .filter(node -> !node.isRoot())
                .map(node -> fromRoot(node, byParent))
                .collect(Collectors.toList());

        return new CategoryTreeDto(root.getId(), root.getName(), childDtos);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CategoryTreeDto> getChildren() { return children; }
    public void setChildren(List<CategoryTreeDto> children) { this.children = children; }
}
