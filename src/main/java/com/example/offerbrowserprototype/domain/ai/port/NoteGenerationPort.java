package com.example.offerbrowserprototype.domain.ai.port;

import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;

public interface NoteGenerationPort {

    NoteContent generate(String title, String shortPrompt, Difficulty difficulty, String promptVersion);
}
