package com.example.knowledgeforge.support;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pełny zestaw zmiennych z .env.example w wersji testowej — testy nie zależą od lokalnego .env.
 * Baza: WYŁĄCZNIE schemat kf_test; schedulery wyłączone; pliki w katalogu tymczasowym.
 */
public final class TestEnv {

    private TestEnv() {
    }

    public static Map<String, String> values(Path tempDir) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("SERVER_PORT", "0");
        v.put("DB_URL", TestDatabase.URL);
        v.put("DB_USERNAME", TestDatabase.USER);
        v.put("DB_PASSWORD", TestDatabase.PASSWORD);
        v.put("DB_POOL_MAX_SIZE", "10");
        v.put("DB_POOL_MIN_IDLE", "1");
        v.put("CORS_ALLOWED_ORIGIN", "http://localhost:3000");
        v.put("FRONTEND_BASE_URL", "http://localhost:3000");
        v.put("JWT_SECRET", "integration-test-secret-integration-test-secret-0123456789");
        v.put("JWT_ISSUER", "knowledge-forge-test");
        v.put("JWT_EXPIRATION_TIME_MS", "3600000");
        v.put("AUTH_REQUIRE_EMAIL_CONFIRMATION", "false");
        v.put("ADMIN_USERNAME", "it-admin");
        v.put("ADMIN_EMAIL", "it-admin@test.local");
        v.put("ADMIN_PASSWORD", "ItAdmin123!");
        v.put("LEGACY_DEFAULT_USERNAME", "it-legacy-default");
        v.put("LEGACY_DEFAULT_USER_TRANSFER_TO", "");
        v.put("MAIL_HOST", "localhost");
        v.put("MAIL_PORT", "2525");
        v.put("MAIL_USERNAME", "");
        v.put("MAIL_PASSWORD", "");
        v.put("OPENAI_API_KEY", "");
        v.put("OPENAI_BASE_URL", "http://localhost:1");
        v.put("OPENAI_MODEL", "gpt-4o-mini");
        v.put("OPENAI_TEMPERATURE", "0.4");
        v.put("AI_DAILY_NOTE_LIMIT", "3");
        v.put("AI_DAILY_QUIZ_LIMIT", "2");
        v.put("AI_QUIZ_MAX_POOL", "250");
        v.put("AI_QUIZ_TIMEOUT_SECONDS", "20");
        v.put("NOTES_STORAGE_PATH", tempDir.resolve("notes").toString());
        v.put("ATTACHMENTS_STORAGE_PATH", tempDir.resolve("attachments").toString());
        v.put("ATTACHMENTS_MAX_FILE_SIZE_MB", "1");
        v.put("DEBUG_LOGGING", "false");
        v.put("BACKUP_ENABLED", "false");
        v.put("BACKUP_DIRECTORY", tempDir.resolve("db-backups").toString());
        v.put("BACKUP_RETENTION_DAYS", "14");
        v.put("BACKUP_SCHEDULE_HOUR", "2");
        v.put("BACKUP_SCHEDULE_MINUTE", "0");
        v.put("BACKUP_PG_DUMP_PATH", "pg_dump");
        v.put("BACKUP_PG_RESTORE_PATH", "pg_restore");
        v.put("BACKUP_RESTORE_ENABLED", "false");
        v.put("BACKUP_RESTORE_SECRET", "");
        v.put("ACTUALITY_VERIFICATION_ENABLED", "false");
        v.put("ACTUALITY_VERIFICATION_PERIOD", "P2Y");
        v.put("ACTUALITY_VERIFICATION_CRON", "0 17 * * 5");
        v.put("ACTUALITY_VERIFICATION_ZONE_ID", "Europe/Warsaw");
        v.put("NOTE_BUNDLE_BACKUP_ENABLED", "false");
        v.put("NOTE_BUNDLE_BACKUP_DIRECTORY", tempDir.resolve("bundle-backups").toString());
        v.put("NOTE_BUNDLE_BACKUP_CRON", "30 17 * * *");
        v.put("NOTE_BUNDLE_BACKUP_ZONE_ID", "Europe/Warsaw");
        v.put("UI_VISIBLE_BINARY_LOADER", "true");
        return v;
    }
}
