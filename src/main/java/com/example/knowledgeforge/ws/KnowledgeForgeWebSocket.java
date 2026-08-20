package com.example.knowledgeforge.ws;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.logging.Logger;

/**
 * Jedna instancja na połączenie (zob. KnowledgeForgeWebSocketServlet#configure, factory.setCreator).
 * Kanał jest jednokierunkowy server->klient: aplikacja nie przyjmuje żadnych poleceń od klienta
 * przez WebSocket (mutacje idą normalnym REST-em) — wiadomości przychodzące są ignorowane.
 */
@WebSocket
public class KnowledgeForgeWebSocket {

    private static final Logger log = Logger.getLogger(KnowledgeForgeWebSocket.class.getName());

    private final WebSocketConnectionRegistry registry;
    private volatile Session session;

    public KnowledgeForgeWebSocket(WebSocketConnectionRegistry registry) {
        this.registry = registry;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        registry.add(session);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        if (session != null) registry.remove(session);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        log.fine(() -> "WS session error: " + cause.getMessage());
        if (session != null) registry.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        // Celowo no-op — zob. komentarz klasy.
    }
}
