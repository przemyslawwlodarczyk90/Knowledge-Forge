package com.example.knowledgeforge.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** /api/admin/** (poza /api/admin/backups) — wymaga roli ADMIN (zob. SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("platform", "Knowledge-Forge", "status", "ready");
    }
}
