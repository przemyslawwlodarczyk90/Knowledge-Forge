package com.example.offerbrowserprototype.domain.ai.stub;

import com.example.offerbrowserprototype.domain.ai.port.NoteGenerationPort;
import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Fallback — active when no real AI service is configured.
 * Replace by setting knowledge.ai.openai.enabled=true and OPENAI_API_KEY.
 */
@Service
@ConditionalOnMissingBean(NoteGenerationPort.class)
public class NoOpNoteGenerationService implements NoteGenerationPort {

    @Override
    public NoteContent generate(String topicTitle, String userInstruction, Difficulty difficulty) {
        throw new UnsupportedOperationException(
            "AI generation not configured. Set knowledge.ai.openai.enabled=true " +
            "and provide OPENAI_API_KEY environment variable."
        );
    }
}
