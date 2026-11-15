package com.example.knowledgeforge.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminBackupControllerSecurityTest {

    @Test
    void isLocalhostAddress_accepts_loopback_forms() {
        assertTrue(AdminBackupController.isLocalhostAddress("127.0.0.1"));
        assertTrue(AdminBackupController.isLocalhostAddress("::1"));
        assertTrue(AdminBackupController.isLocalhostAddress("0:0:0:0:0:0:0:1"));
        assertTrue(AdminBackupController.isLocalhostAddress("0:0:0:0:0:0:0:1%0"), "IPv6 zone id suffix must be tolerated");
        assertTrue(AdminBackupController.isLocalhostAddress("127.0.0.5"), "whole 127.0.0.0/8 range is loopback");
    }

    @Test
    void isLocalhostAddress_rejects_everything_else() {
        assertFalse(AdminBackupController.isLocalhostAddress("10.0.0.5"));
        assertFalse(AdminBackupController.isLocalhostAddress("192.168.1.1"));
        assertFalse(AdminBackupController.isLocalhostAddress(null));
        assertFalse(AdminBackupController.isLocalhostAddress(""));
    }

    @Test
    void secretMatches_requires_nonblank_configured_secret_even_if_header_matches_empty() {
        assertFalse(AdminBackupController.secretMatches("", "anything"));
        assertFalse(AdminBackupController.secretMatches(null, "anything"));
        assertFalse(AdminBackupController.secretMatches("   ", "   "));
    }

    @Test
    void secretMatches_requires_header_present() {
        assertFalse(AdminBackupController.secretMatches("real-secret", null));
    }

    @Test
    void secretMatches_true_only_for_exact_match() {
        assertTrue(AdminBackupController.secretMatches("real-secret", "real-secret"));
        assertFalse(AdminBackupController.secretMatches("real-secret", "wrong-secret"));
        assertFalse(AdminBackupController.secretMatches("real-secret", "real-secre"));
        assertFalse(AdminBackupController.secretMatches("real-secret", "real-secretX"));
    }
}
