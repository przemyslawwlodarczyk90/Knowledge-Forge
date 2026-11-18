package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;

/** Aktywny, gdy OPENAI_API_KEY jest pusty — generowanie quizu kończy się 503 z instrukcją. */
public class NoOpQuizGenerationService implements QuizGenerationPort {

    @Override
    public QuizBatch generate(QuizChunk chunk) {
        throw new AiNotConfiguredException(
                "AI nie jest skonfigurowane — wpisz klucz OpenAI w pliku .env (OPENAI_API_KEY=...) i zrestartuj aplikację.");
    }

    @Override
    public String modelName() {
        return "none";
    }
}
