package com.example.offerbrowserprototype.domain.category.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank String name,
        String description,
        UUID parentId
) {
}
