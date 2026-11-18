package com.example.knowledgeforge.quiz.dto;

/**
 * Wynik "Wygeneruj quiz z AI": ile pytań trafiło do puli (po odrzuceniu niepoprawnych i
 * duplikatów) i aktualny stan puli. {@code failedChunks} > 0 = część porcji się nie udała.
 */
public record QuizGenerationResultDto(int added, int requested, int failedChunks, QuizStatsDto stats,
                                      String model, String promptVersion, int usedToday, int dailyLimit) {
}
