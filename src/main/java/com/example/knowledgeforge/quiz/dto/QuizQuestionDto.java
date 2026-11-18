package com.example.knowledgeforge.quiz.dto;

import com.example.knowledgeforge.quiz.QuizDifficulty;
import com.example.knowledgeforge.quiz.QuizQuestion;

import java.util.List;
import java.util.UUID;

public record QuizQuestionDto(UUID id, QuizDifficulty difficulty, String question, String code,
                              List<String> options, int correctIndex, String explanation) {

    public static QuizQuestionDto of(QuizQuestion q) {
        return new QuizQuestionDto(q.id(), q.difficulty(), q.question(), q.code(), q.options(),
                q.correctIndex(), q.explanation());
    }
}
