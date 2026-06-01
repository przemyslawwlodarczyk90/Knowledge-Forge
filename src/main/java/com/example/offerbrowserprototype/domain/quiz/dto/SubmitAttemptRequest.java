package com.example.offerbrowserprototype.domain.quiz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitAttemptRequest(
        UUID quizId,
        @NotNull List<String> answers
) {
}
