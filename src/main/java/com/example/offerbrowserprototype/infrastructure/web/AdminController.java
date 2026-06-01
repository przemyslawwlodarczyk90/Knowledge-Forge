package com.example.offerbrowserprototype.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin-only endpoints — require ROLE_ADMIN")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Operation(summary = "Admin panel status", description = "Returns panel status. Requires ROLE_ADMIN.")
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "platform", "Knowledge-Forge",
            "status", "ready"
        ));
    }
}
