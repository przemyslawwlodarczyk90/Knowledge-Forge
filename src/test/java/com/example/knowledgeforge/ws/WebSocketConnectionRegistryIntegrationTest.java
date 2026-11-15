package com.example.knowledgeforge.ws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integracyjny test WebSocketConnectionRegistry na PRAWDZIWYM, osadzonym serwerze (Tomcat, efemeryczny
 * port, osobny od aplikacji) + klient WebSocket z JDK — otwiera kilka sesji, broadcastuje, zamyka
 * jedną gwałtownie w trakcie i sprawdza, że reszta i tak dostała wiadomość i że martwa sesja zniknęła
 * z rejestru. Nie startuje żadnej innej części aplikacji (bez bazy, bez security) — id użytkownika
 * sesji podaje parametr ?uid= (w aplikacji robi to AuthenticatedHandshakeInterceptor z JWT).
 */
class WebSocketConnectionRegistryIntegrationTest {

    ConfigurableApplicationContext ctx;
    WebSocketConnectionRegistry registry;
    HttpClient client;
    int port;

    @SpringBootConfiguration
    @EnableWebSocket
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebSocketServletAutoConfiguration.class})
    static class Config implements WebSocketConfigurer {

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            // rejestr tworzy test (pole statyczne) — żeby asercje patrzyły na tę samą instancję
            registry.addHandler(new KnowledgeForgeWebSocketHandler(holder), "/ws/updates")
                    .addInterceptors(new UidFromQuery());
        }

        static WebSocketConnectionRegistry holder;
    }

    static class UidFromQuery implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String uid = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("uid");
            attributes.put(AuthenticatedHandshakeInterceptor.USER_ID_ATTRIBUTE, Long.valueOf(uid));
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }

    @BeforeEach
    void setUp() {
        registry = new WebSocketConnectionRegistry();
        Config.holder = registry;
        ctx = new SpringApplicationBuilder(Config.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0", "spring.main.banner-mode=off")
                .run();
        port = ((WebServerApplicationContext) ctx).getWebServer().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (ctx != null) ctx.close();
    }

    static class RecordingClient implements WebSocket.Listener {
        final List<String> received = new CopyOnWriteArrayList<>();
        volatile WebSocket socket;

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            received.add(data.toString());
            webSocket.request(1);
            return null;
        }
    }

    private RecordingClient connectOne(long uid) throws Exception {
        RecordingClient c = new RecordingClient();
        c.socket = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/updates?uid=" + uid), c)
                .get(5, TimeUnit.SECONDS);
        return c;
    }

    @Test
    void broadcast_reaches_all_connected_sessions_and_survives_an_abrupt_close_mid_flight() throws Exception {
        RecordingClient survivor1 = connectOne(1);
        RecordingClient survivor2 = connectOne(1);
        RecordingClient doomed = connectOne(1);

        waitUntilActiveCount(3);

        // Zamyka jedną sesję gwałtownie (bez czystego handshake'u zamknięcia) — symuluje zerwane
        // łącze/zamkniętą kartę przeglądarki.
        doomed.socket.abort();
        waitUntilActiveCount(2);

        registry.broadcast("{\"type\":\"TEST_EVENT\"}", 1L);

        waitUntilReceived(survivor1, 1);
        waitUntilReceived(survivor2, 1);

        assertEquals(1, survivor1.received.size());
        assertEquals(1, survivor2.received.size());
        assertEquals("{\"type\":\"TEST_EVENT\"}", survivor1.received.get(0));
        assertEquals(2, registry.activeCount(), "dead session must have been dropped from the registry");
    }

    @Test
    void closed_session_is_removed_from_registry_without_affecting_others() throws Exception {
        RecordingClient a = connectOne(1);
        RecordingClient b = connectOne(1);
        waitUntilActiveCount(2);

        a.socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(5, TimeUnit.SECONDS);
        waitUntilActiveCount(1);

        registry.broadcast("{\"type\":\"AFTER_CLOSE\"}", 1L);
        waitUntilReceived(b, 1);
        assertEquals("{\"type\":\"AFTER_CLOSE\"}", b.received.get(0));
    }

    @Test
    void broadcast_reaches_only_sessions_of_the_owning_user() throws Exception {
        RecordingClient owner = connectOne(1);
        RecordingClient stranger = connectOne(2);
        waitUntilActiveCount(2);

        registry.broadcast("{\"type\":\"OWNER_ONLY\"}", 1L);
        waitUntilReceived(owner, 1);
        assertEquals(1, owner.received.size());

        // Kolejne zdarzenie do "obcego" pozwala bez zgadywania czasu potwierdzić, że wcześniejsze do
        // niego nie dotarło (wysyłki do jednej sesji są uporządkowane).
        registry.broadcast("{\"type\":\"FOR_STRANGER\"}", 2L);
        waitUntilReceived(stranger, 1);
        assertEquals(List.of("{\"type\":\"FOR_STRANGER\"}"), stranger.received);
    }

    /** Krótkie aktywne odpytywanie stanu, bo otwarcie/zamknięcie WS jest asynchroniczne (inne wątki I/O). */
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
        assertTrue(c.received.size() >= expectedCount, "expected at least " + expectedCount + " message(s)");
    }
}
