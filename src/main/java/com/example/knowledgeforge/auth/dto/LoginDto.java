package com.example.knowledgeforge.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank(message = "Username cannot be empty") String username,
        @NotBlank(message = "Password cannot be empty") String password
) {
}
