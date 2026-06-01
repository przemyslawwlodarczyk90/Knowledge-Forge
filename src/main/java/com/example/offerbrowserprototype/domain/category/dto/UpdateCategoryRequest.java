package com.example.offerbrowserprototype.domain.category.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank String name,
        String description
) {
}
