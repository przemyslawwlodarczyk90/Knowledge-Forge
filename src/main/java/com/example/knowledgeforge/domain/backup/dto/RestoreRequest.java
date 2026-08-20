package com.example.knowledgeforge.domain.backup.dto;

public record RestoreRequest(String backupFile, String confirmation) {
}
