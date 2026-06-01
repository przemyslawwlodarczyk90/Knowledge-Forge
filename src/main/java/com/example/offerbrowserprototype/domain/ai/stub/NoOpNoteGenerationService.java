package com.example.offerbrowserprototype.domain.ai.stub;

import com.example.offerbrowserprototype.domain.ai.port.NoteGenerationPort;
import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NoOpNoteGenerationService implements NoteGenerationPort {

    @Override
    public NoteContent generate(String title, String shortPrompt, Difficulty difficulty, String promptVersion) {
        throw new UnsupportedOperationException(
                "Spring AI integration not configured. Implement NoteGenerationPort with a real AI client.");
    }
}
