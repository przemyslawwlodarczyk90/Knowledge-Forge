package com.example.knowledgeforge.support;

import com.example.knowledgeforge.actuality.ActualityVerificationService;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.repository.AttachmentRepository;
import com.example.knowledgeforge.repository.CategoryRepository;
import com.example.knowledgeforge.repository.NoteRepository;
import com.example.knowledgeforge.repository.TopicRepository;
import org.springframework.transaction.PlatformTransactionManager;
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
 * Prawdziwe (nie-mockowane) drzewo serwisów do testów integracyjnych/współbieżności — te same
 * konstruktory co w kontenerze Springa (repozytoria z TestJpa), ale na osobnym schemacie testowym (zob. TestDatabase) i tymczasowych
 * katalogach na pliki notatek/załączników. ApplicationEventHub jest realny, lecz jego
 * WebSocketConnectionRegistry nigdy nie ma żadnej sesji dodanej — broadcast() jest wtedy
 * no-opem (pusty zbiór do rozesłania), więc testy serwisów swobodnie wołają metody publikujące
 * zdarzenia bez uruchamiania prawdziwego serwera WebSocket.
 */
public final class ServiceHarness implements AutoCloseable {

    public final HikariDataSource dataSource;
    public final CategoryRepository categoryRepository;
    public final TopicRepository topicRepository;
    public final NoteRepository noteRepository;
    public final AttachmentRepository attachmentRepository;
    public final CurrentUser currentUser;
    public final AttachmentLockRegistry attachmentLockRegistry;
    public final AttachmentService attachmentService;
    public final TopicService topicService;
    public final NoteService noteService;
    public final ApplicationEventHub eventHub;
    public final ActualityVerificationService actualityVerificationService;
    /** Zegar wstrzyknięty do actualityVerificationService — testy przestawiają go przez
     *  {@code clock.set(instant, zone)} zamiast czekać na prawdziwy upływ czasu (zob. MutableClock). */
    public final MutableClock clock;
    public final AppConfig config;

    public ServiceHarness(long userId, Path tempDir) {
        this(userId, tempDir, new Properties());
    }

    public ServiceHarness(long userId, Path tempDir, Properties configOverrides) {
        this.dataSource = TestDatabase.create();
        this.categoryRepository = TestJpa.get(CategoryRepository.class);
        this.topicRepository = TestJpa.get(TopicRepository.class);
        this.noteRepository = TestJpa.get(NoteRepository.class);
        this.attachmentRepository = TestJpa.get(AttachmentRepository.class);
        PlatformTransactionManager tx = TestJpa.transactionManager();
        // Podmienia CurrentUser#id() na stałą wartość bez logowania/JWT — testy serwisów nie
        // przechodzą przez Spring Security.
        this.currentUser = new CurrentUser() {
            @Override
            public Long id() {
                return userId;
            }
        };
        this.eventHub = new ApplicationEventHub(new WebSocketConnectionRegistry());
        this.attachmentLockRegistry = new AttachmentLockRegistry();

        this.config = AppConfig.fromProperties(configOverrides);
        AttachmentStorage attachmentStorage = new AttachmentStorage(tempDir.resolve("attachments"));
        this.attachmentService = new AttachmentService(
                attachmentRepository, attachmentStorage, topicRepository, currentUser, config, attachmentLockRegistry, eventHub);
        this.topicService = new TopicService(tx, topicRepository, categoryRepository, noteRepository, currentUser,
                attachmentService, eventHub);
        NoteFileStorage noteFileStorage = new NoteFileStorage(tempDir.resolve("notes"));
        this.noteService = new NoteService(
                tx, noteRepository, topicService, topicRepository, categoryRepository, currentUser, noteFileStorage, eventHub);
        this.clock = MutableClock.systemUTC();
        this.actualityVerificationService = new ActualityVerificationService(
                topicRepository, config, currentUser, eventHub, clock);
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
        return withFreshUser(tempDir, usernamePrefix, new Properties());
    }

    /** Jak {@link #withFreshUser(Path, String)}, ale z niestandardowymi wartościami config.properties
     *  (np. innym actuality.verification.period — zob. ActualityVerificationServiceTest). */
    public static ServiceHarness withFreshUser(Path tempDir, String usernamePrefix, Properties configOverrides) throws Exception {
        long userId;
        try (HikariDataSource bootstrapDs = TestDatabase.create()) {
            userId = TestFixtures.insertUser(bootstrapDs, usernamePrefix + "-" + java.util.UUID.randomUUID());
        }
        return new ServiceHarness(userId, tempDir, configOverrides);
    }
}
