package com.example.knowledgeforge.auth.dto;

import jakarta.validation.constraints.Email;

/** Aktualizacja profilu ZALOGOWANEGO użytkownika — puste pola zostają bez zmian. */
public record UpdateUserDto(String username, @Email(message = "Email should be valid") String email) {
}
