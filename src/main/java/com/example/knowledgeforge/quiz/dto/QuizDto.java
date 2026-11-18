package com.example.knowledgeforge.quiz.dto;

import java.util.List;

/** Jedno podejście do quizu — świeżo wylosowane pytania z całej puli tematu. */
public record QuizDto(List<QuizQuestionDto> questions, int poolSize, double passRatio) {
}
