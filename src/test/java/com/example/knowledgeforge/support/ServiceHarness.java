package com.example.knowledgeforge.support;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.dao.AttachmentDao;
import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.dao.UserDao;
import com.example.knowledgeforge.service.AttachmentLockRegistry;
import com.example.knowledgeforge.service.AttachmentService;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.TopicService;
import com.example.knowledgeforge.storage.AttachmentStorage;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.example.knowledgeforge.ws.ApplicationEventHub;
import com.example.knowledgeforge.ws.WebSocketConnectionRegistry;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.util.Properties;

/**
 * Prawdziwe (nie-mockowane) drzewo serwisów do testów integracyjnych/współbieżności — ten sam
 * wire-up co Main.java, ale na osobnym schemacie testowym (zob. TestDatabase) i tymczasowych
 * katalogach na pliki notatek/załączników. ApplicationEventHub jest realny, lecz jego
 * WebSocketConnectionRegistry nigdy nie ma żadnej sesji dodanej — broadcast() jest wtedy
 * no-opem (pusty zbiór do rozesłania), więc testy serwisów swobodnie wołają metody publikujące
 * zdarzenia bez uruchamiania prawdziwego serwera WebSocket.
 */
public final class ServiceHarness implements AutoCloseable {

    public final HikariDataSource dataSource;
    public final UserDao userDao;
    public final CategoryDao categoryDao;
    public final TopicDao topicDao;
    public final NoteDao noteDao;
    public final AttachmentDao attachmentDao;
    public final CurrentUser currentUser;
    public final AttachmentLockRegistry attachmentLockRegistry;
    public final AttachmentService attachmentService;
    public final TopicService topicService;
    public final NoteService noteService;
    public final ApplicationEventHub eventHub;

    public ServiceHarness(long userId, Path tempDir) {
        this.dataSource = TestDatabase.create();
        this.userDao = new UserDao(dataSource);
        this.categoryDao = new CategoryDao(dataSource);
        this.topicDao = new TopicDao(dataSource);
        this.noteDao = new NoteDao(dataSource);
        this.attachmentDao = new AttachmentDao(dataSource);
        // Podmienia CurrentUser#id() na stałą wartość bez logowania/sesji HTTP — zgodnie z tym,
        // jak aplikacja i tak działa (jeden domyślny użytkownik), ale bez zależności od
        // DefaultUserSeeder/config w testach.
        this.currentUser = new CurrentUser(userDao, "unused") {
            @Override
            public Long id() {
                return userId;
            }
        };
        this.eventHub = new ApplicationEventHub(new WebSocketConnectionRegistry());
        this.attachmentLockRegistry = new AttachmentLockRegistry();

        AppConfig config = AppConfig.fromProperties(new Properties());
        AttachmentStorage attachmentStorage = new AttachmentStorage(tempDir.resolve("attachments"));
        this.attachmentService = new AttachmentService(
                attachmentDao, attachmentStorage, topicDao, currentUser, config, attachmentLockRegistry, eventHub);
        this.topicService = new TopicService(dataSource, topicDao, categoryDao, currentUser, attachmentService, eventHub);
        NoteFileStorage noteFileStorage = new NoteFileStorage(tempDir.resolve("notes"));
        this.noteService = new NoteService(
                dataSource, noteDao, topicService, topicDao, categoryDao, currentUser, noteFileStorage, eventHub);
    }

    @Override
    public void close() {
        eventHub.shutdown();
        dataSource.close();
    }

    /**
     * Tworzy harness dla świeżego, unikalnego użytkownika testowego — otwiera krótkotrwałe
     * "rozruchowe" połączenie tylko po to, żeby wstawić wiersz w users przed właściwym
     * ServiceHarness (który sam otwiera swój docelowy pool).
     */
    public static ServiceHarness withFreshUser(Path tempDir, String usernamePrefix) throws Exception {
        long userId;
        try (HikariDataSource bootstrapDs = TestDatabase.create()) {
            userId = TestFixtures.insertUser(bootstrapDs, usernamePrefix + "-" + java.util.UUID.randomUUID());
        }
        return new ServiceHarness(userId, tempDir);
    }
}
