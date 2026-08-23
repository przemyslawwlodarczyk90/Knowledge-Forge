package com.example.knowledgeforge.support;

import com.example.knowledgeforge.config.AppConfig;

import java.nio.file.Path;
import java.util.Properties;

public final class TestAppConfigs {

    private TestAppConfigs() {
    }

    public static AppConfig withBackupDir(Path backupDir, int retentionDays) {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:postgresql://localhost:5432/pgDB");
        props.setProperty("db.username", "user");
        props.setProperty("db.password", "password");
        props.setProperty("backup.directory", backupDir.toString());
        props.setProperty("backup.retention-days", String.valueOf(retentionDays));
        props.setProperty("backup.pg-dump-path", "pg_dump");
        props.setProperty("backup.pg-restore-path", "pg_restore");
        return AppConfig.fromProperties(props);
    }

    public static AppConfig withBackupDir(Path backupDir) {
        return withBackupDir(backupDir, 14);
    }

    public static AppConfig withNoteBundleBackup(Path notesDir, Path bundleBackupDir) {
        return withNoteBundleBackup(notesDir, bundleBackupDir, "30 17 * * *", "Europe/Warsaw", true);
    }

    public static AppConfig withNoteBundleBackup(Path notesDir, Path bundleBackupDir, String cron, String zoneId, boolean enabled) {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:postgresql://localhost:5432/pgDB");
        props.setProperty("db.username", "user");
        props.setProperty("db.password", "password");
        props.setProperty("notes.storage.path", notesDir.toString());
        props.setProperty("note-bundle-backup.directory", bundleBackupDir.toString());
        props.setProperty("note-bundle-backup.cron", cron);
        props.setProperty("note-bundle-backup.zone-id", zoneId);
        props.setProperty("note-bundle-backup.enabled", String.valueOf(enabled));
        return AppConfig.fromProperties(props);
    }
}
