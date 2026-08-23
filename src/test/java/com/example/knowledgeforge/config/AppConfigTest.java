package com.example.knowledgeforge.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AppConfig#visibleBinaryLoader — przelacznik widocznosci przycisku "Wczytaj dane binarne"
 * (zob. web.AppConfigServlet, EmergencyRecoveryPanel.jsx). Domyslnie true (widoczny), zeby nie
 * zmieniac istniejacego zachowania bez jawnej konfiguracji.
 */
class AppConfigTest {

    @Test
    void visibleBinaryLoader_defaults_to_true_when_not_configured() {
        AppConfig config = AppConfig.fromProperties(new Properties());
        assertTrue(config.visibleBinaryLoader());
    }

    @Test
    void visibleBinaryLoader_can_be_explicitly_disabled() {
        Properties props = new Properties();
        props.setProperty("ui.visible-binary-loader", "false");
        AppConfig config = AppConfig.fromProperties(props);
        assertFalse(config.visibleBinaryLoader());
    }

    @Test
    void visibleBinaryLoader_can_be_explicitly_enabled() {
        Properties props = new Properties();
        props.setProperty("ui.visible-binary-loader", "true");
        AppConfig config = AppConfig.fromProperties(props);
        assertTrue(config.visibleBinaryLoader());
    }
}
