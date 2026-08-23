package com.example.knowledgeforge.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Period;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Properties;

/**
 * Ładuje config.properties z classpath, opcjonalnie nadpisywany przez
 * config-local.properties (gitignorowany, na sekrety lokalne) i przez
 * zmienne środowiskowe — konwencja: "db.url" -> zmienna "DB_URL".
 * Zastępuje mechanizm @Value / @ConfigurationProperties ze Springa.
 */
public class AppConfig {

    private final Properties props;

    private AppConfig(Properties props) {
        this.props = props;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        loadInto(props, "/config.properties", true);
        loadInto(props, "/config-local.properties", false);
        return new AppConfig(props);
    }

    /** Buduje AppConfig z gotowych Properties, z pominięciem odczytu z classpath — używane przez testy. */
    public static AppConfig fromProperties(Properties props) {
        return new AppConfig(props);
    }

    private static void loadInto(Properties props, String resource, boolean required) {
        try (InputStream in = AppConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                if (required) throw new IllegalStateException("Missing required classpath resource: " + resource);
                return;
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resource, e);
        }
    }

    private String get(String key, String defaultValue) {
        String envKey = key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        return props.getProperty(key, defaultValue);
    }

    public int serverPort() {
        return Integer.parseInt(get("server.port", "8081"));
    }

    public String dbUrl() {
        return get("db.url", "jdbc:postgresql://localhost:5432/pgDB");
    }

    public String dbUsername() {
        return get("db.username", "user");
    }

    public String dbPassword() {
        return get("db.password", "password");
    }

    public String corsAllowedOrigin() {
        return get("cors.allowed-origin", "http://localhost:3000");
    }

    public String defaultUsername() {
        return get("default-user.username", "default");
    }

    public String defaultUserEmail() {
        return get("default-user.email", "default@knowledge-forge.local");
    }

    /** Folder na dysku, gdzie leżą pliki notatek (.kfdoc) i wygenerowane PDF-y — baza trzyma tylko ścieżki. */
    public String notesStoragePath() {
        return get("notes.storage.path", "./data/notes");
    }

    /** Włącza szczegółowe logi (FINE) z pakietu aplikacji — domyślnie włączone, wyłącz w prod przez DEBUG_LOGGING=false. */
    public boolean debugLogging() {
        return Boolean.parseBoolean(get("debug.logging", "true"));
    }

    // ===============================
    // ZAŁĄCZNIKI — pliki na dysku, baza trzyma tylko metadane + względną ścieżkę
    // ===============================

    /** Folder na dysku, gdzie leżą fizyczne pliki załączników (zob. storage.AttachmentStorage). */
    public String attachmentsStoragePath() {
        return get("attachments.storage.path", "./data/attachments");
    }

    /** Maksymalny rozmiar pojedynczego załącznika w MB — egzekwowany zarówno przez Jetty, jak i strumieniowo w AttachmentStorage. */
    public long attachmentsMaxFileSizeMb() {
        return Long.parseLong(get("attachments.max-file-size-mb", "100"));
    }

    // ===============================
    // BACKUP BAZY POSTGRESQL (pg_dump -Fc) — bez backupu plików załączników
    // ===============================

    public boolean backupEnabled() {
        return Boolean.parseBoolean(get("backup.enabled", "true"));
    }

    /** Folder na dysku, gdzie lądują pliki .dump (i tymczasowe .dump.part w trakcie tworzenia). */
    public String backupDirectory() {
        return get("backup.directory", "./data/database-backups");
    }

    public int backupRetentionDays() {
        return Integer.parseInt(get("backup.retention-days", "14"));
    }

    /** Godzina (0-23) codziennego automatycznego backupu, w lokalnej strefie czasowej. */
    public int backupScheduleHour() {
        return Integer.parseInt(get("backup.schedule-hour", "2"));
    }

    /** Minuta (0-59) codziennego automatycznego backupu, w lokalnej strefie czasowej. */
    public int backupScheduleMinute() {
        return Integer.parseInt(get("backup.schedule-minute", "0"));
    }

    /** Ścieżka do binarki pg_dump — domyślnie zakłada, że jest na PATH. */
    public String backupPgDumpPath() {
        return get("backup.pg-dump-path", "pg_dump");
    }

    /** Ścieżka do binarki pg_restore — domyślnie zakłada, że jest na PATH. */
    public String backupPgRestorePath() {
        return get("backup.pg-restore-path", "pg_restore");
    }

    // ===============================
    // ADMINISTRACYJNY ENDPOINT RESTORE — domyślnie WYŁĄCZONY
    // ===============================

    /** Musi być jawnie ustawione na true (BACKUP_RESTORE_ENABLED), inaczej /api/admin/backups/* zwraca 404. */
    public boolean backupRestoreEnabled() {
        return Boolean.parseBoolean(get("backup.restore.enabled", "false"));
    }

    /**
     * Sekret wymagany w nagłówku X-Restore-Secret. CELOWO puste domyślnie — pusty sekret
     * całkowicie blokuje endpoint (zob. AdminBackupServlet), więc nigdy nie trzymamy tu
     * prawdziwej wartości w repozytorium. Produkcyjny sekret ma pochodzić WYŁĄCZNIE
     * ze zmiennej środowiskowej BACKUP_RESTORE_SECRET.
     */
    public String backupRestoreSecret() {
        return get("backup.restore.secret", "");
    }

    // ===============================
    // WERYFIKACJA AKTUALNOŚCI NOTATEK (zob. dokumentacja/ACTUALITY_VERIFICATION.txt)
    // ===============================

    public boolean actualityVerificationEnabled() {
        return Boolean.parseBoolean(get("actuality.verification.enabled", "true"));
    }

    /**
     * Okres ważności jako java.time.Period (kalendarzowy — lata/miesiące/dni, nie sztywna liczba
     * sekund), np. "P2Y" = 2 lata. Parsowane (i więc zwalidowane) przy KAŻDYM wywołaniu — celowo
     * wołane raz, jawnie, zaraz po starcie (zob. Main / ActualityVerificationScheduler), żeby
     * błędny format przerwał uruchomienie aplikacji jednoznacznym wyjątkiem, zamiast po cichu
     * wyłączyć mechanizm dopiero przy pierwszej próbie użycia.
     */
    public Period actualityVerificationPeriod() {
        String raw = get("actuality.verification.period", "P2Y");
        try {
            return Period.parse(raw);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalStateException(
                    "Invalid actuality.verification.period '" + raw + "' — expected an ISO-8601 java.time.Period "
                            + "expression, e.g. P2Y (2 years) or P6M (6 months)", e);
        }
    }

    /** Surowy, pięciopolowy wyraz cron (minuta godzina dzień-miesiąca miesiąc dzień-tygodnia) — parsowany
     *  i walidowany w ActualityVerificationScheduler (jedyne miejsce zależne od cron-utils). */
    public String actualityVerificationCron() {
        return get("actuality.verification.cron", "0 17 * * 5");
    }

    /** Strefa czasowa harmonogramu I samego okresu ważności — jawna, niezależna od strefy serwera/OS. */
    public ZoneId actualityVerificationZoneId() {
        String raw = get("actuality.verification.zone-id", "Europe/Warsaw");
        try {
            return ZoneId.of(raw);
        } catch (java.time.DateTimeException e) {
            throw new IllegalStateException(
                    "Invalid actuality.verification.zone-id '" + raw + "' — expected a valid IANA zone id, "
                            + "e.g. Europe/Warsaw", e);
        }
    }

    // ===============================
    // WIDOCZNOŚĆ ELEMENTÓW UI — przełączniki bez wpływu na samą logikę, tylko na to, czy dany
    // element jest w ogóle pokazany w interfejsie
    // ===============================

    /**
     * Widoczność przycisku "Wczytaj dane binarne" (panel ratunkowy .kfdoc/.kfbundle, zob.
     * dokumentacja/BACKUP_STRATEGY.txt, poziom 4) w menu bocznym. Domyślnie true (widoczny) —
     * ustaw na false, żeby ukryć przycisk bez wyłączania samego mechanizmu w kodzie. Odczytywane
     * przez frontend przez GET /api/app-config (zob. web.AppConfigServlet) jako "visibleBinaryLoader".
     */
    public boolean visibleBinaryLoader() {
        return Boolean.parseBoolean(get("ui.visible-binary-loader", "true"));
    }

    // ===============================
    // CODZIENNY ZBIORCZY BACKUP NOTATEK/INSTRUKCJI (.kfbundle) — zob. dokumentacja/BACKUP_STRATEGY.txt
    // ===============================

    public boolean noteBundleBackupEnabled() {
        return Boolean.parseBoolean(get("note-bundle-backup.enabled", "true"));
    }

    /**
     * Folder na dysku (ŚCIEŻKA SYSTEMOWA, nie URL) dla jedynego, stale nadpisywanego pliku
     * knowledge-forge-notes-latest.kfbundle (i tymczasowego .kfbundle.part w trakcie tworzenia).
     * Celowo INNY katalog niż {@link #notesStoragePath()} — walidowana zgodność w konstruktorze
     * NoteBundleBackupService (zob. tam), nie tutaj.
     */
    public String noteBundleBackupDirectory() {
        return get("note-bundle-backup.directory", "./data/note-bundle-backups");
    }

    /** Surowy, pięciopolowy wyraz cron — parsowany i walidowany w NoteBundleBackupScheduler (jedyne miejsce zależne od cron-utils). */
    public String noteBundleBackupCron() {
        return get("note-bundle-backup.cron", "30 17 * * *");
    }

    /** Strefa czasowa harmonogramu — jawna, niezależna od strefy serwera/OS. */
    public ZoneId noteBundleBackupZoneId() {
        String raw = get("note-bundle-backup.zone-id", "Europe/Warsaw");
        try {
            return ZoneId.of(raw);
        } catch (java.time.DateTimeException e) {
            throw new IllegalStateException(
                    "Invalid note-bundle-backup.zone-id '" + raw + "' — expected a valid IANA zone id, "
                            + "e.g. Europe/Warsaw", e);
        }
    }
}
