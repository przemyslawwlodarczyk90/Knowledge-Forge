package com.example.knowledgeforge.web;

import com.example.knowledgeforge.config.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Mapowany na /api/app-config — GET /api/app-config. Wyłącznie przełączniki WIDOCZNOŚCI
 * elementów UI, bezpieczne do ujawnienia bez uwierzytelnienia (aplikacja i tak działa bez
 * logowania) — NIE sekrety, NIE ścieżki systemowe. Jedno miejsce, przez które frontend odczytuje
 * flagi z config.properties zamiast trzymać je na sztywno.
 */
public class AppConfigServlet extends ApiServlet {

    private final AppConfig config;

    public AppConfigServlet(AppConfig config) {
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeJson(resp, 200, Map.of("visibleBinaryLoader", config.visibleBinaryLoader()));
    }
}
