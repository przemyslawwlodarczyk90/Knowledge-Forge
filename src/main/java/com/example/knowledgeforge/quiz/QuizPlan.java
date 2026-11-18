package com.example.knowledgeforge.quiz;

import java.util.ArrayList;
import java.util.List;

/**
 * Podział jednego "Wygeneruj quiz" (100 pytań) na porcje dla modelu. Jedna odpowiedź z setką
 * pytań przekroczyłaby limit tokenów wyjścia (i trwałaby minuty), więc pytania powstają
 * równolegle w porcjach po maks. {@link #MAX_PER_CHUNK}. Proporcje trudności: 30% łatwe,
 * 40% średnie, 30% trudne. Każda porcja dostaje inną perspektywę pytań, żeby się nie dublowały.
 */
public final class QuizPlan {

    public static final int QUESTIONS_PER_GENERATION = 100;
    public static final int MAX_PER_CHUNK = 20;

    static final List<String> ANGLES = List.of(
            "pojęcia i definicje — ale sprawdzaj ZROZUMIENIE, nie pamięć sformułowań",
            "zastosowanie w praktyce: krótki scenariusz i wybór właściwego rozwiązania",
            "analiza przykładu (kod, wzór, sytuacja): co się stanie, jaki będzie wynik, gdzie jest błąd",
            "rozróżnianie podobnych pojęć i typowe pomyłki",
            "przypadki brzegowe, pułapki i kontrintuicyjne zachowania",
            "przyczyna i skutek: dlaczego coś działa tak, a nie inaczej; konsekwencje decyzji");

    private QuizPlan() {
    }

    public record Part(QuizDifficulty difficulty, int count, String angle) {
    }

    /** Plan porcji dla {@code total} pytań — suma {@code count} równa się dokładnie {@code total}. */
    public static List<Part> split(int total) {
        if (total <= 0) throw new IllegalArgumentException("total must be positive");
        int easy = Math.round(total * 0.3f);
        int hard = Math.round(total * 0.3f);
        int medium = total - easy - hard;

        List<Part> parts = new ArrayList<>();
        addParts(parts, QuizDifficulty.EASY, easy);
        addParts(parts, QuizDifficulty.MEDIUM, medium);
        addParts(parts, QuizDifficulty.HARD, hard);
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            parts.set(i, new Part(p.difficulty(), p.count(), ANGLES.get(i % ANGLES.size())));
        }
        return parts;
    }

    private static void addParts(List<Part> parts, QuizDifficulty difficulty, int count) {
        int left = count;
        while (left > 0) {
            int n = Math.min(MAX_PER_CHUNK, left);
            parts.add(new Part(difficulty, n, null));
            left -= n;
        }
    }
}
