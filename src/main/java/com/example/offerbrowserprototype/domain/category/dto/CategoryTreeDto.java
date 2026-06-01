package com.example.offerbrowserprototype.domain.category.dto;

import com.example.offerbrowserprototype.domain.category.CategoryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CategoryTreeDto {

    private UUID id;
    private String name;
    private String description;
    private List<CategoryTreeDto> children;

    public CategoryTreeDto() {
    }

    public CategoryTreeDto(UUID id, String name, String description, List<CategoryTreeDto> children) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.children = children;
    }

    public static CategoryTreeDto fromRoot(CategoryNode root, Map<UUID, List<CategoryNode>> byParent) {
        List<CategoryNode> directChildren = byParent.getOrDefault(root.getId(), List.of());
        List<CategoryTreeDto> childDtos = directChildren.stream()
                .filter(node -> !node.isRoot())
                .map(node -> fromRoot(node, byParent))
                .collect(Collectors.toList());

        return new CategoryTreeDto(root.getId(), root.getName(), root.getDescription(), childDtos);
    }

    public static CategoryTreeDto buildTree(CategoryNode rootNode, List<CategoryNode> allNodes) {
        Map<UUID, List<CategoryNode>> byParent = allNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryNode::getParentId));

        List<CategoryNode> topLevelCategories = allNodes.stream()
                .filter(n -> !n.isRoot() && n.getParentId() != null && n.getParentId().equals(rootNode.getId()))
                .collect(Collectors.toList());

        List<CategoryTreeDto> childDtos = topLevelCategories.stream()
                .map(node -> fromRoot(node, byParent))
                .collect(Collectors.toList());

        return new CategoryTreeDto(rootNode.getId(), rootNode.getName(), rootNode.getDescription(), childDtos);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CategoryTreeDto> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeDto> children) {
        this.children = children;
    }
}
