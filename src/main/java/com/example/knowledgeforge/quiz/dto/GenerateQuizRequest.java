package com.example.knowledgeforge.quiz.dto;

import jakarta.validation.constraints.Size;

/** Opcjonalna prośba użytkownika o akcenty pytań (np. "więcej pytań o streamy"). */
public record GenerateQuizRequest(@Size(max = 1000) String instruction) {
}
