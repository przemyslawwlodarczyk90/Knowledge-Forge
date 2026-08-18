package com.example.knowledgeforge.domain.category.dto;

import java.util.UUID;

public record CreateCategoryRequest(
        String name,
        UUID parentId
) {
}
