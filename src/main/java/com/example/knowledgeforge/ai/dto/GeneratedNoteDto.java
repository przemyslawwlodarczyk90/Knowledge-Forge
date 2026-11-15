package com.example.knowledgeforge.ai.dto;

import com.example.knowledgeforge.ai.NoteDifficulty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Wygenerowany SZKIC notatki — NIE jest zapisywany. contentJson to dokument edytora (TipTap),
 * gotowy do wczytania i zapisania zwykłym PUT /api/topics/{id}/note.
 */
public record GeneratedNoteDto(UUID topicId, String title, JsonNode contentJson, NoteDifficulty difficulty,
                               String model, String promptVersion, int usedToday, int dailyLimit) {
}
