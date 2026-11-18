package com.example.knowledgeforge.ai.dto;

import com.example.knowledgeforge.ai.NoteDifficulty;

/**
 * Opcjonalne ustawienia tworzenia szkicu. Domyślne wartości pochodzą z tematu.
 */
public record GenerateNoteRequest(String instruction, NoteDifficulty difficulty) {
}
