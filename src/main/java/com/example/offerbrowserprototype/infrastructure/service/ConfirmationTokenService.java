package com.example.offerbrowserprototype.infrastructure.service;

import com.example.offerbrowserprototype.domain.user.ConfirmationToken;
import com.example.offerbrowserprototype.infrastructure.repository.ConfirmationTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ConfirmationTokenService {

    private final ConfirmationTokenRepository tokenRepository;
    private final Clock clock;

    public ConfirmationTokenService(ConfirmationTokenRepository tokenRepository, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }
    public void saveConfirmationToken(ConfirmationToken token) {
        tokenRepository.save(token);
    }

    public Optional<ConfirmationToken> getToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void confirmToken(ConfirmationToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Token has expired");
        }

        token.setConfirmedAt(LocalDateTime.now(clock));
        tokenRepository.save(token);
    }
}