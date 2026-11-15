package com.example.knowledgeforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Zmiana hasła ZALOGOWANEGO użytkownika (konto z tokenu, nie z body). */
public record ChangePasswordDto(
        @NotBlank(message = "Current password cannot be blank") String currentPassword,
        @NotBlank(message = "New password cannot be blank")
        @Size(min = 6, message = "Password should have at least 6 characters")
        String newPassword
) {
}
