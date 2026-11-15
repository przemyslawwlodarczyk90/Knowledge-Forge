package com.example.knowledgeforge.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.user.User;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Wystawianie i weryfikacja JWT (HMAC512) — jak na masterze: subject = username, claim "role".
 * Dodatkowo claim "uid" (id użytkownika), żeby filtr nie musiał szukać konta po nazwie.
 */
@Service
public class JwtService {

    private static final Logger log = Logger.getLogger(JwtService.class.getName());
    private static final int MIN_SECRET_LENGTH = 32;

    private final Algorithm algorithm;
    private final String issuer;
    private final long expirationTimeMs;

    public JwtService(AppConfig config) {
        String secret = config.jwtSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_LENGTH
                    + " characters — set JWT_SECRET or jwt.secret in application-local.properties");
        }
        this.algorithm = Algorithm.HMAC512(secret);
        this.issuer = config.jwtIssuer();
        this.expirationTimeMs = config.jwtExpirationTimeMs();
    }

    public long expirationTimeMs() {
        return expirationTimeMs;
    }

    public String generateToken(User user) {
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        Date now = new Date();
        String token = JWT.create()
                .withSubject(user.getUsername())
                .withClaim("uid", user.getId())
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expirationTimeMs))
                .withIssuer(issuer)
                .sign(algorithm);
        log.info(() -> "Token generated for user: " + user.getUsername());
        return token;
    }

    /** Zweryfikowany (podpis, issuer, ważność) token albo empty — nigdy nie rzuca. */
    public Optional<DecodedJWT> verify(String token) {
        try {
            return Optional.of(JWT.require(algorithm).withIssuer(issuer).build().verify(token));
        } catch (JWTVerificationException e) {
            log.fine(() -> "Invalid JWT: " + e.getMessage());
            return Optional.empty();
        }
    }
}
