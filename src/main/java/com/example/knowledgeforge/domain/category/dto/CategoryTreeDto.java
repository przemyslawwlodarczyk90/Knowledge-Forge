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
    /** Suma tematów w tej kategorii ORAZ we wszystkich jej podkategoriach (rekurencyjnie).
     *  Cyferka w UI wyświetla to tylko na kategoriach głównych (depth 0) — podkategorie jej nie pokazują. */
    private int topicCount;
    /** Z powyższej sumy — ile ma niski poziom szczegółowości (domyślnie ukryte w drzewie jako "do uzupełnienia"). */
    private int lowDetailCount;

    public CategoryTreeDto() {
    }

    public CategoryTreeDto(UUID id, String name, List<CategoryTreeDto> children, int topicCount, int lowDetailCount) {
        this.id = id;
        this.name = name;
        this.children = children;
        this.topicCount = topicCount;
        this.lowDetailCount = lowDetailCount;
    }

    public static CategoryTreeDto fromRoot(CategoryNode root, Map<UUID, List<CategoryNode>> byParent,
                                            Map<UUID, Integer> directTopicCounts, Map<UUID, Integer> directLowDetailCounts) {
        List<CategoryNode> directChildren = byParent.getOrDefault(root.getId(), List.of());
        List<CategoryTreeDto> childDtos = directChildren.stream()
                .filter(node -> !node.isRoot())
                .map(node -> fromRoot(node, byParent, directTopicCounts, directLowDetailCounts))
                .collect(Collectors.toList());

        int ownCount = directTopicCounts.getOrDefault(root.getId(), 0);
        int totalCount = ownCount + childDtos.stream().mapToInt(CategoryTreeDto::getTopicCount).sum();

        int ownLowCount = directLowDetailCounts.getOrDefault(root.getId(), 0);
        int totalLowCount = ownLowCount + childDtos.stream().mapToInt(CategoryTreeDto::getLowDetailCount).sum();

        return new CategoryTreeDto(root.getId(), root.getName(), childDtos, totalCount, totalLowCount);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CategoryTreeDto> getChildren() { return children; }
    public void setChildren(List<CategoryTreeDto> children) { this.children = children; }

    public int getTopicCount() { return topicCount; }
    public void setTopicCount(int topicCount) { this.topicCount = topicCount; }

    public int getLowDetailCount() { return lowDetailCount; }
    public void setLowDetailCount(int lowDetailCount) { this.lowDetailCount = lowDetailCount; }
}
