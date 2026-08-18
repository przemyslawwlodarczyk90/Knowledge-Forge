package com.example.knowledgeforge.config;

import java.io.IOException;
import java.io.InputStream;
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
}
