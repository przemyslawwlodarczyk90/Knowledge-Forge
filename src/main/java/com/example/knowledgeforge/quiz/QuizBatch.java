package com.example.knowledgeforge.quiz;

import java.util.List;

/**
 * Surowa, ustrukturyzowana odpowiedź modelu dla jednej porcji pytań (kształt JSON wymuszany przez
 * Spring AI). Nic tu nie jest jeszcze zaufane — walidację i czyszczenie robi {@link QuizDraftValidator}.
 */
public record QuizBatch(List<Item> questions) {

    public record Item(QuizDifficulty difficulty, String question, String code, List<String> options,
                       Integer correctIndex, String explanation) {
    }
}
