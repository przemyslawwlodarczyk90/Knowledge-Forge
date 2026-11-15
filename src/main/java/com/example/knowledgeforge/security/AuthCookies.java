package com.example.knowledgeforge.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Ciasteczko z tym samym JWT, co zwracany w body logowania. Potrzebne tam, gdzie przeglądarka
 * sama wysyła żądanie i nie dołoży nagłówka Authorization: obrazki w notatkach (&lt;img src="/api/..."&gt;),
 * wideo/pobieranie załączników (&lt;video&gt;, &lt;a href&gt;) i handshake WebSocketu. HttpOnly — skrypt go nie
 * odczyta; SameSite=Strict + akceptowane WYŁĄCZNIE dla GET/HEAD (zob. JwtAuthFilter), więc nie
 * otwiera drogi do CSRF — operacje modyfikujące zawsze wymagają nagłówka Authorization.
 */
public final class AuthCookies {

    public static final String NAME = "KF_TOKEN";

    private AuthCookies() {
    }

    public static ResponseCookie issue(String token, long maxAgeMs, boolean secure) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
