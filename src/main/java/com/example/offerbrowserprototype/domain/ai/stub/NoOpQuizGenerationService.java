package com.example.offerbrowserprototype.domain.ai.stub;

import com.example.offerbrowserprototype.domain.ai.port.QuizGenerationPort;
import com.example.offerbrowserprototype.domain.quiz.QuizContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NoOpQuizGenerationService implements QuizGenerationPort {

    @Override
    public QuizContent generate(String noteTitle, String noteContentJson, Difficulty difficulty,
                                int questionCount, String promptVersion) {
        throw new UnsupportedOperationException(
                "Spring AI integration not configured. Implement QuizGenerationPort with a real AI client.");
    }
}
