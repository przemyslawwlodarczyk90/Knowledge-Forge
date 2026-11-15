package com.example.knowledgeforge.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Configuration;

/**
 * debug.logging=true (domyślnie) -> szczegółowe logi (DEBUG, czyli dawne java.util.logging FINE)
 * z pakietu aplikacji. Wyłącz w prod przez DEBUG_LOGGING=false. Kod dalej loguje przez
 * java.util.logging — Spring Boot przekierowuje to do Logbacka (jul-to-slf4j) i propaguje poziomy.
 */
@Configuration
public class DebugLoggingConfig {

    private final AppConfig config;
    private final LoggingSystem loggingSystem;

    public DebugLoggingConfig(AppConfig config, LoggingSystem loggingSystem) {
        this.config = config;
        this.loggingSystem = loggingSystem;
    }

    @PostConstruct
    void apply() {
        if (config.debugLogging()) {
            loggingSystem.setLogLevel("com.example.knowledgeforge", LogLevel.DEBUG);
        }
    }
}
