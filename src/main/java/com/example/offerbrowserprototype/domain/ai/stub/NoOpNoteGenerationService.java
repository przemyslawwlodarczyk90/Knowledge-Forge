package com.example.offerbrowserprototype.domain.ai.stub;

import com.example.offerbrowserprototype.domain.ai.port.NoteGenerationPort;
import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Fallback — aktywny gdy knowledge.ai.openai.enabled=false lub brak wpisu.
 * Zastąp wpisując enabled=true i podając OPENAI_API_KEY.
 */
@Service
@ConditionalOnProperty(
    name = "knowledge.ai.openai.enabled",
    havingValue = "false",
    matchIfMissing = true   // brak wpisu = false = no-op aktywny
)
public class NoOpNoteGenerationService implements NoteGenerationPort {

    @Override
    public NoteContent generate(String topicTitle, String userInstruction, Difficulty difficulty) {
        throw new UnsupportedOperationException(
            "AI nie jest skonfigurowane. " +
            "Otwórz src/main/resources/application-local.properties, " +
            "wklej klucz i zmień knowledge.ai.openai.enabled=true."
        );
    }
}
