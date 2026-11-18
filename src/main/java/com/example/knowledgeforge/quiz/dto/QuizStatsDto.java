package com.example.knowledgeforge.quiz.dto;

/** Stan puli pytań tematu — liczby do znaczka w panelu notatki i ekranu startowego quizu. */
public record QuizStatsDto(int total, int easy, int medium, int hard, int maxPool,
                           int questionsPerGeneration, double passRatio) {
}
