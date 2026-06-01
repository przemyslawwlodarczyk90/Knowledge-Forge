package com.example.offerbrowserprototype.domain.dto.loginandregister;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserDto {

    @NotNull(message = "User ID cannot be null")
    private Long id;

    private String username;

    @Email
    private String email;
}
