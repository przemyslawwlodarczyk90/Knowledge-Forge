package com.example.offerbrowserprototype.domain.dto.loginandregister;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResultDTO {
    private Long userId;
    private String username;
    private boolean isSuccess;
    private String message;

    public RegistrationResultDTO(String username, boolean isSuccess, String message) {
        this.username = username;
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
