package com.example.knowledgeforge.auth;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID_LINK = "Link do resetowania hasła jest nieprawidłowy lub wygasł.";

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig config;
    private final Clock clock;

    public PasswordResetService(UserRepository users, JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                                AppConfig config, Clock clock) {
        this.users = users;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.config = config;
        this.clock = clock;
    }

    public record ResetEmail(String address, String link) {}

    @Transactional
    public Optional<ResetEmail> request(String username) {
        var user = users.findByUsername(username).orElse(null);
        if (user == null || user.getPassword() == null || user.getEmail().isBlank()
                || (config.requireEmailConfirmation() && !user.isActive())) {
            return Optional.empty();
        }

        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        OffsetDateTime now = OffsetDateTime.now(clock);
        int updated = jdbc.update("""
                INSERT INTO password_reset_tokens (user_id, token_hash, credentials_hash, requested_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET token_hash = EXCLUDED.token_hash,
                    credentials_hash = EXCLUDED.credentials_hash, requested_at = EXCLUDED.requested_at,
                    expires_at = EXCLUDED.expires_at
                WHERE password_reset_tokens.requested_at <= ?
                """, user.getId(), hash(token), credentialsHash(user.getEmail(), user.getPassword()),
                now, now.plusMinutes(30), now.minusMinutes(1));

        String baseUrl = config.frontendBaseUrl().replaceFirst("/+$", "");
        return updated == 0 ? Optional.empty()
                : Optional.of(new ResetEmail(user.getEmail(), baseUrl + "/reset-password#token=" + token));
    }

    @Transactional
    public void reset(String token, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Hasło może mieć maksymalnie 72 bajty w UTF-8.");
        }

        var claimed = jdbc.query("""
                DELETE FROM password_reset_tokens WHERE token_hash = ? AND expires_at > ?
                RETURNING user_id, credentials_hash
                """, (row, index) -> new ResetClaim(row.getLong("user_id"), row.getString("credentials_hash")),
                hash(token), OffsetDateTime.now(clock));
        if (claimed.isEmpty()) {
            throw new ValidationException(INVALID_LINK);
        }

        ResetClaim claim = claimed.get(0);
        var user = users.findById(claim.userId()).orElseThrow(() -> new ValidationException(INVALID_LINK));
        if (!claim.credentialsHash().equals(credentialsHash(user.getEmail(), user.getPassword()))) {
            throw new ValidationException(INVALID_LINK);
        }

        int updated = jdbc.update("UPDATE users SET password = ? WHERE id = ? AND email = ? AND password = ?",
                passwordEncoder.encode(password), user.getId(), user.getEmail(), user.getPassword());
        if (updated == 0) {
            throw new ValidationException(INVALID_LINK);
        }
    }

    private record ResetClaim(long userId, String credentialsHash) {}

    private static String credentialsHash(String email, String password) {
        return hash(email + "\0" + password);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
