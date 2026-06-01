package com.example.offerbrowserprototype.domain.progress.dto;

public record ProgressSummaryDto(
        long totalTopics,
        long passedTopics,
        long totalPoints,
        double averageScore
) {
}
