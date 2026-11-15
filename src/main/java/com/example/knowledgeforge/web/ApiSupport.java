package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Wspólne pomocnicze z dawnego ApiServlet — parsowanie id ze ścieżki z identycznymi komunikatami
 * błędów (400 "Invalid id: ...") i odczyt nagłówka X-Client-Id.
 */
final class ApiSupport {

    private ApiSupport() {
    }

    static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + value);
        }
    }

    /** Załączniki mają id BIGSERIAL (Long), nie UUID jak reszta encji. */
    static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid id: " + value);
        }
    }

    /**
     * Identyfikator karty przeglądarki (sessionStorage po stronie frontendu) — pozwala karcie,
     * która sama wykonała operację, rozpoznać i zignorować własne zdarzenie WebSocket
     * (zob. ws.ApplicationEventHub, WsEvent#sourceClientId). Brak nagłówka = null.
     */
    static String clientId(HttpServletRequest req) {
        String value = req.getHeader("X-Client-Id");
        return (value == null || value.isBlank()) ? null : value;
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
