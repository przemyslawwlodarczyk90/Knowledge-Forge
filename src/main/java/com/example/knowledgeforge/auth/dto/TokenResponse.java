package com.example.knowledgeforge.auth.dto;

/** Odpowiedź logowania — ten sam kształt, co na masterze (frontend czyta token, userId, username, email, role). */
public record TokenResponse(String token, Long userId, String username, String email, String role) {
}
