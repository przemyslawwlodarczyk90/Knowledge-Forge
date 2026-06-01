package com.example.offerbrowserprototype.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "confirmation_tokens",
        indexes = {
                @Index(name = "idx_token_unique", columnList = "token", unique = true)
        })
public class ConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private Long userId; // ⬅️ ZMIENIONO z String na Long

    // Konstruktor używany w UserRegistrationHandler
    public ConfirmationToken(String token, LocalDateTime createdAt,
                             LocalDateTime expiresAt, Long userId) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.userId = userId;
    }
}