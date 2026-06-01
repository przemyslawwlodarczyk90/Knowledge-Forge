package com.example.offerbrowserprototype.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-time-ms}")
    private long expirationTimeMs;

    @Value("${jwt.issuer}")
    private String issuer;

    private Algorithm getAlgorithm() {
        try {
            return Algorithm.HMAC512(secretKey);
        } catch (IllegalArgumentException e) {
            logger.error("Error in secret key format: {}", e.getMessage());
            throw e;
        }
    }

    public String extractUsername(String token) {
        logger.debug("Extracting username from token");
        try {
            DecodedJWT jwt = getDecodedJWT(token);
            String username = jwt.getSubject();
            logger.debug("Extracted username: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = getDecodedJWT(token);
            Date expiration = jwt.getExpiresAt();
            boolean isExpired = expiration.before(new Date());
            logger.debug("Token expiration date: {}, Is expired: {}", expiration, isExpired);
            return isExpired;
        } catch (Exception e) {
            logger.error("Failed to check if token is expired: {}", e.getMessage());
            throw e;
        }
    }

    private DecodedJWT getDecodedJWT(String token) {
        try {
            Algorithm algorithm = getAlgorithm();
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            logger.debug("Token successfully decoded for subject: {}", jwt.getSubject());
            return jwt;
        } catch (Exception e) {
            logger.error("Error while decoding token: {}", e.getMessage());
            throw e;
        }
    }

    public String generateToken(UserDetails userDetails) {
        logger.info("Generating token for user: {}", userDetails.getUsername());
        try {
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");

            Algorithm algorithm = getAlgorithm();
            String token = JWT.create()
                    .withSubject(userDetails.getUsername())
                    .withClaim("role", role)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + expirationTimeMs))
                    .withIssuer(issuer)
                    .sign(algorithm);
            logger.info("Token generated successfully for user: {}", userDetails.getUsername());
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate token: {}", e.getMessage());
            throw e;
        }
    }

    public String extractRole(String token) {
        try {
            String role = getDecodedJWT(token).getClaim("role").asString();
            return role != null ? role : "USER";
        } catch (Exception e) {
            return "USER";
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        logger.info("Validating token for user: {}", userDetails.getUsername());
        try {
            DecodedJWT jwt = getDecodedJWT(token);
            String username = jwt.getSubject();
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            logger.info("Token validation result for user {}: {}", userDetails.getUsername(), isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Invalid JWT Token: {}", e.getMessage());
            return false;
        }
    }
}