package com.example.knowledgeforge;

import com.example.knowledgeforge.actuality.ActualityVerificationScheduler;
import com.example.knowledgeforge.actuality.ActualityVerificationService;
import com.example.knowledgeforge.backup.DatabaseBackupService;
import com.example.knowledgeforge.backup.DatabaseRestoreService;
import com.example.knowledgeforge.backup.MaintenanceGate;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.config.Database;
import com.example.knowledgeforge.config.DefaultUserSeeder;
import com.example.knowledgeforge.config.Schema;
import com.example.knowledgeforge.dao.AttachmentDao;
import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.dao.UserDao;
import com.example.knowledgeforge.service.AttachmentDiagnosticsService;
import com.example.knowledgeforge.service.AttachmentLockRegistry;
import com.example.knowledgeforge.service.AttachmentService;
import com.example.knowledgeforge.service.CategoryService;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.SearchService;
import com.example.knowledgeforge.service.TopicService;
import com.example.knowledgeforge.storage.AttachmentStorage;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.example.knowledgeforge.web.AdminBackupServlet;
import com.example.knowledgeforge.web.AttachmentServlet;
import com.example.knowledgeforge.web.CategoryServlet;
import com.example.knowledgeforge.web.CorsFilter;
import com.example.knowledgeforge.web.MaintenanceModeFilter;
import com.example.knowledgeforge.web.SearchServlet;
import com.example.knowledgeforge.web.SpaServlet;
import com.example.knowledgeforge.web.TopicServlet;
import com.example.knowledgeforge.ws.ApplicationEventHub;
import com.example.knowledgeforge.ws.KnowledgeForgeWebSocketServlet;
import com.example.knowledgeforge.ws.WebSocketConnectionRegistry;
import com.example.knowledgeforge.ws.WebSocketHeartbeat;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Punkt wejścia i "composition root" — całe drzewo obiektów (DAO -> serwisy
 * -> serwlety) jest tu ręcznie spięte przez zwykłe konstruktory. Bez
 * kontenera IoC, bez reflection, bez adnotacji — to, co widać, to
 * wszystko, co się dzieje.
 */
public final class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        if (config.debugLogging()) {
            configureDebugLogging();
        }

        HikariDataSource dataSource = Database.createDataSource(config);
        Schema.ensure(dataSource);

        // ── WebSocket — rejestr sesji, hub zdarzeń, heartbeat ────────
        WebSocketConnectionRegistry wsRegistry = new WebSocketConnectionRegistry();
        ApplicationEventHub eventHub = new ApplicationEventHub(wsRegistry);
        WebSocketHeartbeat heartbeat = new WebSocketHeartbeat(wsRegistry);

        // ── DAO ──────────────────────────────────────────────────
        UserDao userDao = new UserDao(dataSource);
        CategoryDao categoryDao = new CategoryDao(dataSource);
        TopicDao topicDao = new TopicDao(dataSource);
        NoteDao noteDao = new NoteDao(dataSource);
        AttachmentDao attachmentDao = new AttachmentDao(dataSource);

        DefaultUserSeeder.ensure(userDao, config);
        CurrentUser currentUser = new CurrentUser(userDao, config.defaultUsername());
        NoteFileStorage noteFileStorage = new NoteFileStorage(Path.of(config.notesStoragePath()));
        // Konstruktor tworzy katalog załączników, jeśli nie istnieje — jeśli się nie da,
        // rzuca i przerywa start (zob. AttachmentStorage).
        AttachmentStorage attachmentStorage = new AttachmentStorage(Path.of(config.attachmentsStoragePath()));

        // ── Serwisy ──────────────────────────────────────────────
        AttachmentLockRegistry attachmentLockRegistry = new AttachmentLockRegistry();
        AttachmentService attachmentService = new AttachmentService(
                attachmentDao, attachmentStorage, topicDao, currentUser, config, attachmentLockRegistry, eventHub);
        TopicService topicService = new TopicService(
                dataSource, topicDao, categoryDao, currentUser, attachmentService, eventHub);
        CategoryService categoryService = new CategoryService(categoryDao, topicDao, currentUser, eventHub);
        NoteService noteService = new NoteService(
                dataSource, noteDao, topicService, topicDao, categoryDao, currentUser, noteFileStorage, eventHub);
        SearchService searchService = new SearchService(topicDao, noteDao, currentUser, noteFileStorage);

        // Diagnostyka spójności baza<->dysk — tylko raportuje do logów, nic nie usuwa.
        new AttachmentDiagnosticsService(attachmentDao, attachmentStorage).run();

        // ── Weryfikacja aktualności notatek (zob. ACTUALITY_VERIFICATION.txt) ────────────────
        // Scheduler waliduje okres/cron/strefę w KONSTRUKTORZE, bezwarunkowo — tworzony jest więc
        // zawsze (nie tylko gdy enabled=true), żeby błędna konfiguracja zawsze przerywała start.
        ActualityVerificationService actualityVerificationService = new ActualityVerificationService(
                dataSource, topicDao, config, currentUser, eventHub, Clock.systemUTC());
        ActualityVerificationScheduler actualityVerificationScheduler =
                new ActualityVerificationScheduler(config, actualityVerificationService);
        actualityVerificationScheduler.start();

        // ── Backup / restore ────────────────────────────────────
        MaintenanceGate maintenanceGate = new MaintenanceGate();
        // Konstruktor tworzy katalog backupów, jeśli nie istnieje — jeśli się nie da,
        // rzuca i przerywa start (zob. DatabaseBackupService).
        DatabaseBackupService backupService = new DatabaseBackupService(config, maintenanceGate);
        backupService.start();
        DatabaseRestoreService restoreService = new DatabaseRestoreService(config, maintenanceGate, backupService, dataSource);

        // ── HTTP (Jetty embedded + zwykłe serwlety) ─────────────────
        Server server = new Server(config.serverPort());
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");

        ctx.addFilter(new FilterHolder(new CorsFilter(config.corsAllowedOrigin())), "/*",
                EnumSet.of(DispatcherType.REQUEST));
        // Blokuje żądania modyfikujące dane na czas pg_restore (zob. DatabaseRestoreService).
        ctx.addFilter(new FilterHolder(new MaintenanceModeFilter(maintenanceGate)), "/*",
                EnumSet.of(DispatcherType.REQUEST));

        // Rejestruje komponent Jetty odpowiedzialny za upgrade HTTP -> WebSocket na tym kontekście —
        // wymagane raz, przed rejestracją jakiegokolwiek JettyWebSocketServlet.
        JettyWebSocketServletContainerInitializer.configure(ctx, null);
        ctx.addServlet(new ServletHolder(new KnowledgeForgeWebSocketServlet(wsRegistry)), "/ws/updates");

        ServletHolder topicServletHolder = new ServletHolder(
                new TopicServlet(topicService, noteService, attachmentService, actualityVerificationService));
        long maxAttachmentBytes = config.attachmentsMaxFileSizeMb() * 1024L * 1024L;
        // fileSizeThreshold=0 -> Jetty od razu spilluje każdą część multipart na dysk zamiast
        // buforować duże pliki w pamięci; TopicServlet dalej kopiuje ją strumieniowo do
        // docelowego katalogu (zob. AttachmentStorage#save — bez byte[] całego pliku).
        topicServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"), maxAttachmentBytes, maxAttachmentBytes + 1_048_576L, 0));
        ctx.addServlet(topicServletHolder, "/api/topics/*");

        ctx.addServlet(new ServletHolder(new CategoryServlet(categoryService, topicService)), "/api/categories/*");
        ctx.addServlet(new ServletHolder(new SearchServlet(searchService)), "/api/search");
        ctx.addServlet(new ServletHolder(new AttachmentServlet(attachmentService)), "/api/attachments/*");
        // Sekretny endpoint admina — świadomie bez wpisu w żadnym menu/UI (zob. AdminBackupServlet).
        ctx.addServlet(new ServletHolder(new AdminBackupServlet(config, backupService, restoreService)), "/api/admin/backups/*");
        ctx.addServlet(new ServletHolder(new SpaServlet()), "/");

        server.setHandler(ctx);
        server.start();

        heartbeat.start(Duration.ofSeconds(25));

        log.info("Knowledge Forge 2.0 running on http://localhost:" + config.serverPort() + " (no login required)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Kolejność ma znaczenie: najpierw przestajemy przyjmować nowe żądania/połączenia
            // (server.stop()), potem zamykamy to, co jeszcze wisi (WS, schedulery), i dopiero
            // na końcu bazę — żeby żadne wciąż działające zadanie nie sięgnęło po już zamknięty
            // HikariDataSource.
            try {
                server.stop();
            } catch (Exception e) {
                log.warning("Error while stopping Jetty: " + e.getMessage());
            }
            wsRegistry.closeAll();
            heartbeat.shutdown();
            eventHub.shutdown();
            backupService.shutdown();
            actualityVerificationScheduler.shutdown();
            dataSource.close();
        }));

        server.join();
    }

    /**
     * java.util.logging domyślnie pokazuje tylko INFO+ — bez tego wszystkie
     * log.fine(...) rozsiane po DAO/serwisach/serwletach byłyby niewidoczne.
     * Włączone domyślnie (debug.logging=true); wyłącz w prod przez DEBUG_LOGGING=false.
     */
    private static void configureDebugLogging() {
        Logger appLogger = Logger.getLogger("com.example.knowledgeforge");
        appLogger.setLevel(Level.FINE);
        appLogger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        handler.setFormatter(new SimpleFormatter());
        appLogger.addHandler(handler);
    }

    private Main() {
    }
}
