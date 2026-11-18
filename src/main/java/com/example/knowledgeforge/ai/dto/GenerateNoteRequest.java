package com.example.knowledgeforge.ai.dto;

import com.example.knowledgeforge.ai.NoteDifficulty;
import jakarta.validation.constraints.Size;

/**
 * Bez currentNote powstaje nowy szkic. Z currentNote model dopracowuje przekazany tekst
 * zgodnie z wymaganymi w tym trybie wytycznymi instruction.
 */
public record GenerateNoteRequest(String instruction, NoteDifficulty difficulty,
                                  @Size(max = 60_000) String currentNote) {
}
