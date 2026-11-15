package com.example.knowledgeforge.web;

import com.example.knowledgeforge.config.AppConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/app-config — wyłącznie przełączniki WIDOCZNOŚCI elementów UI, bezpieczne do ujawnienia
 * bez uwierzytelnienia — NIE sekrety, NIE ścieżki systemowe.
 */
@RestController
public class AppConfigController {

    private final AppConfig config;

    public AppConfigController(AppConfig config) {
        this.config = config;
    }

    @GetMapping("/api/app-config")
    public Map<String, Object> get() {
        return Map.of("visibleBinaryLoader", config.visibleBinaryLoader());
    }
}
