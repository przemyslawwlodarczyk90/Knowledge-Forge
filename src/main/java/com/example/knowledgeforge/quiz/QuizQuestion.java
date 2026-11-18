package com.example.knowledgeforge.quiz;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pytanie jednokrotnego wyboru z puli quizu tematu. Zawsze cztery odpowiedzi (A–D), dokładnie
 * jedna poprawna ({@code correctIndex}). {@code code} — opcjonalny fragment kodu / wzoru, do
 * którego odnosi się pytanie (wyświetlany jako blok kodu nad odpowiedziami).
 */
public record QuizQuestion(UUID id, UUID topicId, QuizDifficulty difficulty, String question, String code,
                           List<String> options, int correctIndex, String explanation, Instant createdAt) {
}
