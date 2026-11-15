package com.example.knowledgeforge.ai.dto;

import com.example.knowledgeforge.ai.NoteDifficulty;

/**
 * Body POST /api/topics/{id}/note/generate — oba pola opcjonalne: instruction (brak = opis tematu),
 * difficulty (brak = wyliczona z poziomu szczegółowości tematu).
 */
public record GenerateNoteRequest(String instruction, NoteDifficulty difficulty) {
}
