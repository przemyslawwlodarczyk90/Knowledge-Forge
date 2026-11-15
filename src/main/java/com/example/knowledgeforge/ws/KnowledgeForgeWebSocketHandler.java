package com.example.knowledgeforge.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Logger;

/**
 * Kanał /ws/updates jest jednokierunkowy server->klient: aplikacja nie przyjmuje żadnych poleceń
 * przez WebSocket (mutacje idą normalnym REST-em) — wiadomości przychodzące są ignorowane.
 * Id właściciela sesji ustawia {@link AuthenticatedHandshakeInterceptor} przy handshake'u.
 */
@Component
public class KnowledgeForgeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = Logger.getLogger(KnowledgeForgeWebSocketHandler.class.getName());

    private final WebSocketConnectionRegistry registry;

    public KnowledgeForgeWebSocketHandler(WebSocketConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(AuthenticatedHandshakeInterceptor.USER_ID_ATTRIBUTE);
        registry.add(session, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable cause) {
        log.fine(() -> "WS session error: " + cause.getMessage());
        registry.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Celowo no-op — zob. komentarz klasy.
    }
}
