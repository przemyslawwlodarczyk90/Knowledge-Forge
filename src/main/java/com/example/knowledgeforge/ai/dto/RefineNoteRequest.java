package com.example.knowledgeforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefineNoteRequest(
        @NotBlank @Size(max = 1000) String instruction,
        @NotBlank @Size(max = 60_000) String currentNote
) {}
