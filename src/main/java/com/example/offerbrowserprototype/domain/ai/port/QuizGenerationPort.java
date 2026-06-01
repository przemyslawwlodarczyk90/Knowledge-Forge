package com.example.offerbrowserprototype.domain.ai.port;

import com.example.offerbrowserprototype.domain.quiz.QuizContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;

public interface QuizGenerationPort {

    QuizContent generate(String noteTitle, String noteContentJson, Difficulty difficulty, int questionCount, String promptVersion);
}
