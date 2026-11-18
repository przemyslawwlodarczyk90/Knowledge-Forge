package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;

/** Aktywny, gdy OPENAI_API_KEY jest pusty — każda próba generowania kończy się 503 z instrukcją. */
public class NoOpNoteGenerationService implements NoteGenerationPort {

    @Override
    public NoteContent generate(String topicTitle, String userInstruction, NoteDifficulty difficulty) {
        throw new AiNotConfiguredException(
                "AI nie jest skonfigurowane — wpisz klucz OpenAI w pliku .env (OPENAI_API_KEY=...) i zrestartuj aplikację.");
    }

    @Override
    public NoteContent refine(String topicTitle, String userInstruction, NoteDifficulty difficulty, String currentNote) {
        return generate(topicTitle, userInstruction, difficulty);
    }

    @Override
    public String modelName() {
        return "none";
    }
}
