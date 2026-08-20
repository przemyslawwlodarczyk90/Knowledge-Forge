package com.example.knowledgeforge.ws;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integracyjny test WebSocketConnectionRegistry na PRAWDZIWYM, osadzonym Jetty (efemeryczny port,
 * osobny od aplikacji) + websocket-jetty-client (test-scope w pom.xml) — otwiera kilka sesji,
 * broadcastuje, zamyka jedną gwałtownie w trakcie i sprawdza, że reszta i tak dostała wiadomość
 * i że martwa sesja zniknęła z rejestru. Nie startuje żadnej innej części aplikacji (bez bazy).
 */
class WebSocketConnectionRegistryIntegrationTest {

    Server server;
    WebSocketConnectionRegistry registry;
    WebSocketClient client;
    int port;

    @BeforeEach
    void setUp() throws Exception {
        registry = new WebSocketConnectionRegistry();

        server = new Server(0);
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");
        JettyWebSocketServletContainerInitializer.configure(ctx, null);
        ctx.addServlet(new ServletHolder(new KnowledgeForgeWebSocketServlet(registry)), "/ws/updates");
        server.setHandler(ctx);
        server.start();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

        client = new WebSocketClient();
        client.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) client.stop();
        if (server != null) server.stop();
    }

    // public — Jetty client resolves @OnWebSocket* handlers via MethodHandles.Lookup, which
    // requires the class itself (not just its methods) to be accessible from its own module.
    @WebSocket
    public static class RecordingClient {
        final CountDownLatch connected = new CountDownLatch(1);
        final List<String> received = new CopyOnWriteArrayList<>();
        volatile Session session;

        @OnWebSocketConnect
        public void onConnect(Session session) {
            this.session = session;
            connected.countDown();
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            received.add(message);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
        }
    }

    private RecordingClient connectOne() throws Exception {
        RecordingClient c = new RecordingClient();
        client.connect(c, URI.create("ws://localhost:" + port + "/ws/updates")).get(5, TimeUnit.SECONDS);
        assertTrue(c.connected.await(5, TimeUnit.SECONDS), "client must connect within timeout");
        return c;
    }

    @Test
    void broadcast_reaches_all_connected_sessions_and_survives_an_abrupt_close_mid_flight() throws Exception {
        RecordingClient survivor1 = connectOne();
        RecordingClient survivor2 = connectOne();
        RecordingClient doomed = connectOne();

        waitUntilActiveCount(3);

        // Zamyka jedną sesję gwałtownie (bez czystego handshake'u zamknięcia) W TRAKCIE, zanim
        // broadcast zdąży dotrzeć — symuluje zerwane łącze/zamkniętą kartę przeglądarki.
        doomed.session.disconnect();
        waitUntilActiveCount(2);

        registry.broadcast("{\"type\":\"TEST_EVENT\"}");

        waitUntilReceived(survivor1, 1);
        waitUntilReceived(survivor2, 1);

        assertEquals(1, survivor1.received.size());
        assertEquals(1, survivor2.received.size());
        assertEquals("{\"type\":\"TEST_EVENT\"}", survivor1.received.get(0));
        assertEquals(2, registry.activeCount(), "dead session must have been dropped from the registry");
    }

    @Test
    void closed_session_is_removed_from_registry_without_affecting_others() throws Exception {
        RecordingClient a = connectOne();
        RecordingClient b = connectOne();
        waitUntilActiveCount(2);

        a.session.close();
        waitUntilActiveCount(1);

        registry.broadcast("{\"type\":\"AFTER_CLOSE\"}");
        waitUntilReceived(b, 1);
        assertEquals("{\"type\":\"AFTER_CLOSE\"}", b.received.get(0));
    }

    /** Bez Thread.sleep w pętli-zgadywance z góry narzuconym czasem — krótkie aktywne odpytywanie
     *  stanu, bo otwarcie/zamknięcie WS jest asynchroniczne (inne wątki Jetty I/O). */
    private void waitUntilActiveCount(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (registry.activeCount() != expected && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(expected, registry.activeCount());
    }

    private void waitUntilReceived(RecordingClient c, int expectedCount) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (c.received.size() < expectedCount && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }
}
