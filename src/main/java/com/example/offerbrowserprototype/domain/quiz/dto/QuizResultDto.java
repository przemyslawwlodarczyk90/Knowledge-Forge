package com.example.offerbrowserprototype.domain.quiz.dto;

import java.util.UUID;

public record QuizResultDto(
        UUID attemptId,
        int correctAnswers,
        int totalQuestions,
        int scorePercent,
        boolean passed,
        int pointsEarned
) {
}
