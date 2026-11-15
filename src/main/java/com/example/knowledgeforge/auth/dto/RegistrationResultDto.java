package com.example.knowledgeforge.auth.dto;

public record RegistrationResultDto(Long userId, String username, boolean success, String message) {
}
