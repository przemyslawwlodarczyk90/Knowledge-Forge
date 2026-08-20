package com.example.knowledgeforge.web;

import com.example.knowledgeforge.backup.BackupAuditLog;
import com.example.knowledgeforge.backup.DatabaseBackupService;
import com.example.knowledgeforge.backup.DatabaseRestoreService;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.backup.dto.RestoreRequest;
import com.example.knowledgeforge.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Mapowany na /api/admin/backups/*. Sekretny, administracyjny endpoint — CELOWO bez żadnej
 * obsługi w UI, nigdzie na frontendzie nielinkowany. Bezpieczeństwo NIE opiera się wyłącznie
 * na tym, że nikt "nie zna" adresu:
 *   1. backup.restore.enabled musi być jawnie true, inaczej WSZYSTKO tutaj wygląda jak 404
 *      (nieodróżnialne od trasy, która w ogóle nie istnieje),
 *   2. tylko żądania z localhosta (127.0.0.1 / ::1) — bez zaufania X-Forwarded-For,
 *   3. nagłówek X-Restore-Secret porównywany stało-czasowo (MessageDigest.isEqual),
 *      pusty skonfigurowany sekret CAŁKOWICIE blokuje endpoint,
 *   4. restore dodatkowo wymaga dokładnej frazy potwierdzenia w body.
 * Każda odrzucona i każda wykonana próba restore trafia do audytu (BackupAuditLog) —
 * bez sekretu i bez hasła bazy.
 */
public class AdminBackupServlet extends ApiServlet {

    private static final String CONFIRMATION_PHRASE = "RESTORE KNOWLEDGE-FORGE";

    private final AppConfig config;
    private final DatabaseBackupService backupService;
    private final DatabaseRestoreService restoreService;

    public AdminBackupServlet(AppConfig config, DatabaseBackupService backupService, DatabaseRestoreService restoreService) {
        this.config = config;
        this.backupService = backupService;
        this.restoreService = restoreService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!config.backupRestoreEnabled()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!isLocalhost(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "LIST_BACKUPS", null, "REJECTED_NOT_LOCALHOST");
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!secretMatches(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "LIST_BACKUPS", null, "REJECTED_BAD_SECRET");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String[] seg = pathSegments(req);
        if (seg.length == 0) {
            writeJson(resp, 200, backupService.listBackups());
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!config.backupRestoreEnabled()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] seg = pathSegments(req);
        if (seg.length != 1 || !"restore".equals(seg[0])) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!isLocalhost(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_NOT_LOCALHOST");
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!secretMatches(req)) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_BAD_SECRET");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        RestoreRequest body;
        try {
            body = readJson(req, RestoreRequest.class);
        } catch (RuntimeException e) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_BAD_BODY");
            writeError(resp, 400, "Malformed request body");
            return;
        }
        if (body == null || !CONFIRMATION_PHRASE.equals(body.confirmation())) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE",
                    body != null ? body.backupFile() : null, "REJECTED_BAD_CONFIRMATION");
            writeError(resp, 400, "Missing or incorrect confirmation phrase");
            return;
        }
        if (body.backupFile() == null || body.backupFile().isBlank()) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", null, "REJECTED_MISSING_FILENAME");
            writeError(resp, 400, "backupFile is required");
            return;
        }

        DatabaseRestoreService.RestoreResult result;
        try {
            result = restoreService.restore(body.backupFile());
        } catch (ValidationException e) {
            BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", body.backupFile(), "REJECTED_INVALID_FILE");
            throw e; // -> 400 przez wspólny handler w ApiServlet
        }

        BackupAuditLog.record(req.getRemoteAddr(), "RESTORE", body.backupFile(), result.outcome().name());

        switch (result.outcome()) {
            case SUCCESS -> writeJson(resp, 200,
                    Map.of("status", "success", "restartRequired", false, "message", result.message()));
            case SUCCESS_RESTART_REQUIRED -> writeJson(resp, 200,
                    Map.of("status", "success", "restartRequired", true, "message", result.message()));
            case CONFLICT -> writeError(resp, 409, result.message());
            case EMERGENCY_BACKUP_FAILED, RESTORE_FAILED -> writeError(resp, 500, result.message());
        }
    }

    /** Wyłącznie loopback — świadomie NIE czytamy X-Forwarded-For (aplikacja nie ma skonfigurowanego zaufanego reverse proxy). */
    private boolean isLocalhost(HttpServletRequest req) {
        return isLocalhostAddress(req.getRemoteAddr());
    }

    /** Stało-czasowe porównanie (MessageDigest.isEqual) — pusty skonfigurowany sekret zawsze blokuje. */
    private boolean secretMatches(HttpServletRequest req) {
        return secretMatches(config.backupRestoreSecret(), req.getHeader("X-Restore-Secret"));
    }

    // ── Czyste funkcje (bez HttpServletRequest) — łatwe do przetestowania jednostkowo ──

    /**
     * Parsuje przez InetAddress zamiast porównywać z sztywną listą stringów — Jetty potrafi
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
