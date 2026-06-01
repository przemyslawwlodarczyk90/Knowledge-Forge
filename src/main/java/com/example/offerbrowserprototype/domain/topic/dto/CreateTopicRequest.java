package com.example.offerbrowserprototype.domain.topic.dto;

import com.example.offerbrowserprototype.domain.topic.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTopicRequest(
        UUID categoryId,
        @NotBlank String title,
        @NotBlank String shortPrompt,
        @NotNull Difficulty difficulty,
        boolean code
) {
}
