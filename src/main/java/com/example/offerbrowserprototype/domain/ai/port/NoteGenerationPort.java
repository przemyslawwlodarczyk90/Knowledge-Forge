package com.example.offerbrowserprototype.domain.ai.port;

import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;

public interface NoteGenerationPort {

    /**
     * Generates a NoteContent for the given topic using AI.
     *
     * @param topicTitle      title of the topic / subject
     * @param userInstruction additional context entered by the user (shortPrompt)
     * @param difficulty      desired note depth: BASIC, MEDIUM, HARD
     */
    NoteContent generate(String topicTitle, String userInstruction, Difficulty difficulty);
}
