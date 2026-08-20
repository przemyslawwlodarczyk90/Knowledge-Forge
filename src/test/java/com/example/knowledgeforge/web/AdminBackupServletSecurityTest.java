package com.example.knowledgeforge.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminBackupServletSecurityTest {

    @Test
    void isLocalhostAddress_accepts_loopback_forms() {
        assertTrue(AdminBackupServlet.isLocalhostAddress("127.0.0.1"));
        assertTrue(AdminBackupServlet.isLocalhostAddress("::1"));
        assertTrue(AdminBackupServlet.isLocalhostAddress("0:0:0:0:0:0:0:1"));
        assertTrue(AdminBackupServlet.isLocalhostAddress("0:0:0:0:0:0:0:1%0"), "IPv6 zone id suffix must be tolerated");
        assertTrue(AdminBackupServlet.isLocalhostAddress("127.0.0.5"), "whole 127.0.0.0/8 range is loopback");
    }

    @Test
    void isLocalhostAddress_rejects_everything_else() {
        assertFalse(AdminBackupServlet.isLocalhostAddress("10.0.0.5"));
        assertFalse(AdminBackupServlet.isLocalhostAddress("192.168.1.1"));
        assertFalse(AdminBackupServlet.isLocalhostAddress(null));
        assertFalse(AdminBackupServlet.isLocalhostAddress(""));
    }

    @Test
    void secretMatches_requires_nonblank_configured_secret_even_if_header_matches_empty() {
        assertFalse(AdminBackupServlet.secretMatches("", "anything"));
        assertFalse(AdminBackupServlet.secretMatches(null, "anything"));
        assertFalse(AdminBackupServlet.secretMatches("   ", "   "));
    }

    @Test
    void secretMatches_requires_header_present() {
        assertFalse(AdminBackupServlet.secretMatches("real-secret", null));
    }

    @Test
    void secretMatches_true_only_for_exact_match() {
        assertTrue(AdminBackupServlet.secretMatches("real-secret", "real-secret"));
        assertFalse(AdminBackupServlet.secretMatches("real-secret", "wrong-secret"));
        assertFalse(AdminBackupServlet.secretMatches("real-secret", "real-secre"));
        assertFalse(AdminBackupServlet.secretMatches("real-secret", "real-secretX"));
    }
}
