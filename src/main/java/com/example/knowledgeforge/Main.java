package com.example.knowledgeforge;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.config.Database;
import com.example.knowledgeforge.config.DefaultUserSeeder;
import com.example.knowledgeforge.config.Schema;
import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.dao.UserDao;
import com.example.knowledgeforge.service.CategoryService;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.SearchService;
import com.example.knowledgeforge.service.TopicService;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.example.knowledgeforge.web.CategoryServlet;
import com.example.knowledgeforge.web.CorsFilter;
import com.example.knowledgeforge.web.SearchServlet;
import com.example.knowledgeforge.web.SpaServlet;
import com.example.knowledgeforge.web.TopicServlet;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.nio.file.Path;
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

        // ── DAO ──────────────────────────────────────────────────
        UserDao userDao = new UserDao(dataSource);
        CategoryDao categoryDao = new CategoryDao(dataSource);
        TopicDao topicDao = new TopicDao(dataSource);
        NoteDao noteDao = new NoteDao(dataSource);

        DefaultUserSeeder.ensure(userDao, config);
        CurrentUser currentUser = new CurrentUser(userDao, config.defaultUsername());
        NoteFileStorage noteFileStorage = new NoteFileStorage(Path.of(config.notesStoragePath()));

        // ── Serwisy ──────────────────────────────────────────────
        TopicService topicService = new TopicService(topicDao, categoryDao, currentUser);
        CategoryService categoryService = new CategoryService(categoryDao, topicDao, currentUser);
        NoteService noteService = new NoteService(noteDao, topicService, categoryDao, currentUser, noteFileStorage);
        SearchService searchService = new SearchService(topicDao, noteDao, currentUser, noteFileStorage);

        // ── HTTP (Jetty embedded + zwykłe serwlety) ─────────────────
        Server server = new Server(config.serverPort());
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");

        ctx.addFilter(new FilterHolder(new CorsFilter(config.corsAllowedOrigin())), "/*",
                EnumSet.of(DispatcherType.REQUEST));

        ctx.addServlet(new ServletHolder(new TopicServlet(topicService, noteService)), "/api/topics/*");
        ctx.addServlet(new ServletHolder(new CategoryServlet(categoryService, topicService)), "/api/categories/*");
        ctx.addServlet(new ServletHolder(new SearchServlet(searchService)), "/api/search");
        ctx.addServlet(new ServletHolder(new SpaServlet()), "/");

        server.setHandler(ctx);
        server.start();

        log.info("Knowledge-Forge running on http://localhost:" + config.serverPort() + " (no login required)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception e) {
                log.warning("Error while stopping Jetty: " + e.getMessage());
            }
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
