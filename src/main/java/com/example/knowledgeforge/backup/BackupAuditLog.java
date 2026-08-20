package com.example.knowledgeforge.backup;

import java.time.Instant;
import java.util.logging.Logger;

/**
 * Audyt operacji na sekretnym endpointzie restore — czas, adres żądania, nazwa backupu, wynik.
 * NIGDY sekretu ani hasła bazy. Wszystkie linie zaczynają się od "AUDIT " dla łatwego grepowania.
 */
public final class BackupAuditLog {

    private static final Logger log = Logger.getLogger("com.example.knowledgeforge.AUDIT");

    private BackupAuditLog() {
    }

    public static void record(String remoteAddr, String action, String backupFile, String result) {
        log.info(() -> "AUDIT time=" + Instant.now()
                + " remoteAddr=" + remoteAddr
                + " action=" + action
                + " backupFile=" + (backupFile == null || backupFile.isBlank() ? "-" : backupFile)
                + " result=" + result);
    }
}
