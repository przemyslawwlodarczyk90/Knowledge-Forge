package com.example.offerbrowserprototype.domain.dto.confirmationtoken;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class ConfirmationTokenDTO {

    @NotNull
    private Long id;

    @NotNull
    private String token;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    @NotNull
    private String userId;

    public ConfirmationTokenDTO(@NotNull Long id, String token, LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime confirmedAt, String userId) {
        this.id = id;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
        this.userId = userId;
    }
}
