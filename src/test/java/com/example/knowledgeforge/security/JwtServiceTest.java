package com.example.knowledgeforge.security;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.user.Role;
import com.example.knowledgeforge.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static JwtService service(String secret, String issuer, long ttl) {
        Properties p = new Properties();
        p.setProperty("jwt.secret", secret);
        p.setProperty("jwt.issuer", issuer);
        p.setProperty("jwt.expiration-time-ms", String.valueOf(ttl));
        return new JwtService(AppConfig.fromProperties(p));
    }

    static User user() {
        User u = new User();
        u.setId(42L);
        u.setUsername("ala");
        u.setRole(Role.ADMIN);
        return u;
    }

    static final String SECRET = "0123456789abcdef0123456789abcdef-secret";

    @Test
    void token_round_trip_carries_subject_uid_and_role() {
        JwtService s = service(SECRET, "kf", 60_000);
        var jwt = s.verify(s.generateToken(user())).orElseThrow();
        assertThat(jwt.getSubject()).isEqualTo("ala");
        assertThat(jwt.getClaim("uid").asLong()).isEqualTo(42L);
        assertThat(jwt.getClaim("role").asString()).isEqualTo("ADMIN");
        assertThat(s.expirationTimeMs()).isEqualTo(60_000);
    }

    @Test
    void rejects_tampered_foreign_expired_and_garbage_tokens() {
        JwtService s = service(SECRET, "kf", 60_000);
        String token = s.generateToken(user());
        assertThat(s.verify(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(service(SECRET + "-other", "kf", 60_000).verify(token)).isEmpty();
        assertThat(service(SECRET, "other-issuer", 60_000).verify(token)).isEmpty();
        assertThat(s.verify(service(SECRET, "kf", -1_000).generateToken(user()))).isEmpty();
        assertThat(s.verify("garbage")).isEmpty();
    }

    @Test
    void too_short_secret_stops_startup() {
        assertThatThrownBy(() -> service("short", "kf", 1000)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void cookies_are_http_only_strict_and_clearable() {
        var c = AuthCookies.issue("tok", 5000, true);
        assertThat(c.isHttpOnly()).isTrue();
        assertThat(c.isSecure()).isTrue();
        assertThat(c.getSameSite()).isEqualTo("Strict");
        assertThat(c.getMaxAge().toSeconds()).isEqualTo(5);
        assertThat(AuthCookies.clear(false).getMaxAge().isZero()).isTrue();
    }
}
