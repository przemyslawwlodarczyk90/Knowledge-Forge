package com.example.knowledgeforge.web;

import com.example.knowledgeforge.backup.BackupAuditLog;
import com.example.knowledgeforge.backup.DatabaseBackupService;
import com.example.knowledgeforge.backup.DatabaseRestoreService;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.backup.dto.RestoreRequest;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * /api/admin/backups — sekretny, administracyjny endpoint — CELOWO bez żadnej obsługi w UI.
 * Ma WŁASNE zabezpieczenie, niezależne od JWT (SecurityConfig go przepuszcza):
 *   1. backup.restore.enabled musi być jawnie true, inaczej WSZYSTKO tutaj wygląda jak 404,
 *   2. tylko żądania z localhosta (127.0.0.1 / ::1) — bez zaufania X-Forwarded-For,
 *   3. nagłówek X-Restore-Secret porównywany stało-czasowo (MessageDigest.isEqual),
 *      pusty skonfigurowany sekret CAŁKOWICIE blokuje endpoint,
 *   4. restore dodatkowo wymaga dokładnej frazy potwierdzenia w body.
 * Każda odrzucona i każda wykonana próba restore trafia do audytu (BackupAuditLog) —
 * bez sekretu i bez hasła bazy.
 */
@RestController
@RequestMapping("/api/admin/backups")
public class AdminBackupController {

    private static final String CONFIRMATION_PHRASE = "RESTORE KNOWLEDGE-FORGE";

    private final AppConfig config;
    private final DatabaseBackupService backupService;
    private final DatabaseRestoreService restoreService;

    public AdminBackupController(AppConfig config, DatabaseBackupService backupService,
                                 DatabaseRestoreService restoreService) {
        this.config = config;
        this.backupService = backupService;
        this.restoreService = restoreService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest req) {
        if (!config.backupRestoreEnabled()) {
            return status(HttpStatus.NOT_FOUND);
        }
        if (!isLocalhost(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "LIST_BACKUPS", null, "REJECTED_NOT_LOCALHOST");
            return status(HttpStatus.FORBIDDEN);
        }
        if (!secretMatches(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "LIST_BACKUPS", null, "REJECTED_BAD_SECRET");
            return status(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(backupService.listBackups());
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(HttpServletRequest req) {
        if (!config.backupRestoreEnabled()) {
            return status(HttpStatus.NOT_FOUND);
        }
        if (!isLocalhost(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_NOT_LOCALHOST");
            return status(HttpStatus.FORBIDDEN);
        }
        if (!secretMatches(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_BAD_SECRET");
            return status(HttpStatus.UNAUTHORIZED);
        }

        RestoreRequest body;
        try (InputStream in = req.getInputStream()) {
            body = JsonMapper.get().readValue(in, RestoreRequest.class);
        } catch (IOException | RuntimeException e) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_BAD_BODY");
            return error(HttpStatus.BAD_REQUEST, "Malformed request body");
        }
        if (body == null || !CONFIRMATION_PHRASE.equals(body.confirmation())) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE",
                    body != null ? body.backupFile() : null, "REJECTED_BAD_CONFIRMATION");
            return error(HttpStatus.BAD_REQUEST, "Missing or incorrect confirmation phrase");
        }
        if (body.backupFile() == null || body.backupFile().isBlank()) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_MISSING_FILENAME");
            return error(HttpStatus.BAD_REQUEST, "backupFile is required");
        }

        DatabaseRestoreService.RestoreResult result;
        try {
            result = restoreService.restore(body.backupFile());
        } catch (ValidationException e) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", body.backupFile(), "REJECTED_INVALID_FILE");
            throw e; // -> 400 przez GlobalExceptionHandler
        }

        BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", body.backupFile(), result.outcome().name());

        return switch (result.outcome()) {
            case SUCCESS -> ResponseEntity.ok(
                    Map.of("status", "success", "restartRequired", false, "message", result.message()));
            case SUCCESS_RESTART_REQUIRED -> ResponseEntity.ok(
                    Map.of("status", "success", "restartRequired", true, "message", result.message()));
            case CONFLICT -> error(HttpStatus.CONFLICT, result.message());
            case EMERGENCY_BACKUP_FAILED, RESTORE_FAILED -> error(HttpStatus.INTERNAL_SERVER_ERROR, result.message());
        };
    }

    /** Wyłącznie loopback — świadomie NIE czytamy X-Forwarded-For (aplikacja nie ma skonfigurowanego zaufanego reverse proxy). */
    private boolean isLocalhost(HttpServletRequest req) {
        return isLocalhostAddress(req.getRemoteAddr());
    }

    /** Stało-czasowe porównanie (MessageDigest.isEqual) — pusty skonfigurowany sekret zawsze blokuje. */
    private boolean secretMatches(HttpServletRequest req) {
        return secretMatches(config.backupRestoreSecret(), req.getHeader("X-Restore-Secret"));
    }

    private static ResponseEntity<?> status(HttpStatus status) {
        return ResponseEntity.status(status).build();
    }

    private static ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message == null ? "" : message));
    }

    // ── Czyste funkcje (bez HttpServletRequest) — łatwe do przetestowania jednostkowo ──

    /**
     * Parsuje przez InetAddress zamiast porównywać z sztywną listą stringów — kontener potrafi
     * zwrócić IPv6 loopback w kilku różnych reprezentacjach ("::1", "0:0:0:0:0:0:0:1", czasem
     * z sufiksem strefy "%0"), a #isLoopbackAddress() poprawnie obejmuje cały zakres 127.0.0.0/8
     * i wszystkie warianty ::1 naraz. getByName() na literale IP nie robi DNS-lookupu.
     */
    static boolean isLocalhostAddress(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) return false;
        String addr = remoteAddr;
        int zoneSep = addr.indexOf('%');
        if (zoneSep >= 0) addr = addr.substring(0, zoneSep);
        try {
            return InetAddress.getByName(addr).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    static boolean secretMatches(String configuredSecret, String providedHeader) {
        if (configuredSecret == null || configuredSecret.isBlank()) return false;
        if (providedHeader == null) return false;
        return MessageDigest.isEqual(
                configuredSecret.getBytes(StandardCharsets.UTF_8),
                providedHeader.getBytes(StandardCharsets.UTF_8));
    }
}
