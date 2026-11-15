package com.example.knowledgeforge.config;

import com.example.knowledgeforge.backup.DatabaseBackupService;
import com.example.knowledgeforge.backup.DatabaseRestoreService;
import com.example.knowledgeforge.backup.MaintenanceGate;
import com.example.knowledgeforge.backup.NoteBundleBackupScheduler;
import com.example.knowledgeforge.backup.NoteBundleBackupService;
import com.example.knowledgeforge.json.JsonMapper;
import com.example.knowledgeforge.service.AttachmentLockRegistry;
import com.example.knowledgeforge.storage.AttachmentStorage;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Beany dla klas, które nie mają adnotacji Springa (storage, backup, schedulery z własnymi
 * wątkami) — te same konstruktory i ta sama kolejność startu/zamknięcia, co w dawnym Main.
 * Schedulery walidują konfigurację w KONSTRUKTORZE, bezwarunkowo, więc błędna konfiguracja
 * dalej przerywa start aplikacji.
 */
@Configuration
public class ApplicationBeans {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Ta sama instancja Jacksona dla HTTP i dla wewnętrznych formatów (.kfdoc, zdarzenia WS). */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.get();
    }

    @Bean
    public NoteFileStorage noteFileStorage(AppConfig config) {
        return new NoteFileStorage(Path.of(config.notesStoragePath()));
    }

    /** Konstruktor tworzy katalog załączników, jeśli nie istnieje — jeśli się nie da, rzuca i przerywa start. */
    @Bean
    public AttachmentStorage attachmentStorage(AppConfig config) {
        return new AttachmentStorage(Path.of(config.attachmentsStoragePath()));
    }

    @Bean
    public AttachmentLockRegistry attachmentLockRegistry() {
        return new AttachmentLockRegistry();
    }

    /**
     * fileSizeThreshold=0 -> kontener od razu spilluje każdą część multipart na dysk zamiast
     * buforować duże pliki w pamięci; TopicController dalej kopiuje ją strumieniowo do docelowego
     * katalogu (zob. AttachmentStorage#save). Limity jak wcześniej: plik = max-file-size-mb,
     * całe żądanie = plik + 1 MB.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement(AppConfig config) {
        long maxAttachmentBytes = config.attachmentsMaxFileSizeMb() * 1024L * 1024L;
        return new MultipartConfigElement(System.getProperty("java.io.tmpdir"),
                maxAttachmentBytes, maxAttachmentBytes + 1_048_576L, 0);
    }

    // ── Backup / restore ────────────────────────────────────

    @Bean
    public MaintenanceGate maintenanceGate() {
        return new MaintenanceGate();
    }

    /** Konstruktor tworzy katalog backupów, jeśli nie istnieje — jeśli się nie da, rzuca i przerywa start. */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DatabaseBackupService databaseBackupService(AppConfig config, MaintenanceGate gate) {
        return new DatabaseBackupService(config, gate);
    }

    @Bean
    public DatabaseRestoreService databaseRestoreService(AppConfig config, MaintenanceGate gate,
                                                         DatabaseBackupService backupService,
                                                         HikariDataSource dataSource) {
        return new DatabaseRestoreService(config, gate, backupService, dataSource);
    }

    // ── Awaryjny, codzienny zbiorczy backup .kfdoc -> .kfbundle (zob. dokumentacja/BACKUP_STRATEGY.txt) ──

    @Bean
    public NoteBundleBackupService noteBundleBackupService(AppConfig config) {
        return new NoteBundleBackupService(config);
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public NoteBundleBackupScheduler noteBundleBackupScheduler(AppConfig config, NoteBundleBackupService service) {
        return new NoteBundleBackupScheduler(config, service);
    }
}
