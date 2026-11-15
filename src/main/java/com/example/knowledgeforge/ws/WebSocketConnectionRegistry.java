package com.example.knowledgeforge.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// MULTI-THREADING:
// Kontener obsługuje otwarcia/zamknięcia i błędy WebSocket na różnych wątkach (nie na wątku
// żądania HTTP, który wywołuje broadcast() po commicie transakcji). ConcurrentHashMap daje
// bezpieczny współbieżnie rejestr sesji BEZ globalnego locka — dodanie/usunięcie jednej sesji nie
// blokuje ani innych sesji, ani trwającego broadcastu. Iteracja korzysta z "weakly consistent"
// iteratora: nie rzuci ConcurrentModificationException nawet jeśli w trakcie wysyłki jakaś sesja się
// zamknie. Uszkodzona/zamknięta sesja jest usuwana od razu, a wysyłka do pozostałych leci dalej.
//
// Springowy WebSocketSession NIE jest bezpieczny dla równoległych wysyłek (broadcast z wątku
// ws-broadcast i ping z wątku ws-heartbeat mogą trafić na tę samą sesję jednocześnie) — dlatego
// każda sesja jest owijana w ConcurrentWebSocketSessionDecorator, który serializuje wysyłki.
//
// Dane są per użytkownik, więc każda sesja pamięta id swojego (zalogowanego przy handshake'u)
// właściciela — zdarzenie trafia wyłącznie do sesji użytkownika, którego dane dotyczy.
@Component
public class WebSocketConnectionRegistry {

    private static final Logger log = Logger.getLogger(WebSocketConnectionRegistry.class.getName());
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    /** klucz: id oryginalnej sesji kontenera. */
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    private record Entry(WebSocketSession session, Long userId) {
    }

    public void add(WebSocketSession session, Long userId) {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        sessions.put(session.getId(), new Entry(safe, userId));
        log.fine(() -> "WS session opened: " + session.getRemoteAddress() + " user=" + userId
                + " (active=" + sessions.size() + ")");
    }

    public void remove(WebSocketSession session) {
        boolean removed = sessions.remove(session.getId()) != null;
        if (removed) {
            log.fine(() -> "WS session removed (active=" + sessions.size() + ")");
        }
    }

    public int activeCount() {
        return sessions.size();
    }

    /**
     * Rozsyła tekstową wiadomość do wszystkich aktywnych sesji danego użytkownika. Błąd wysyłki do
     * JEDNEGO klienta jest łapany per-sesja i nie przerywa wysyłki do reszty — uszkodzona sesja jest
     * od razu usuwana z rejestru.
     */
    public void broadcast(String json, Long userId) {
        TextMessage message = new TextMessage(json);
        sessions.forEach((id, entry) -> {
            if (!Objects.equals(entry.userId(), userId)) return;
            try {
                if (entry.session().isOpen()) {
                    entry.session().sendMessage(message);
                } else {
                    sessions.remove(id);
                }
            } catch (Exception e) {
                log.log(Level.FINE, "WS broadcast: failed to send to one session, removing it", e);
                sessions.remove(id);
            }
        });
    }

    /** Wysyła ping (kontrola aktywności) do wszystkich sesji — wołane przez jeden współdzielony scheduler (zob. WebSocketHeartbeat). */
    public void pingAll() {
        sessions.forEach((id, entry) -> {
            try {
                if (entry.session().isOpen()) {
                    entry.session().sendMessage(new PingMessage(ByteBuffer.allocate(0)));
                } else {
                    sessions.remove(id);
                }
            } catch (Exception e) {
                log.log(Level.FINE, "WS heartbeat: failed to ping one session, removing it", e);
                sessions.remove(id);
            }
        });
    }

    /** Wołane przy zamykaniu aplikacji — zamyka wszystkie sesje w kontrolowany sposób. */
    @jakarta.annotation.PreDestroy
    public void closeAll() {
        sessions.forEach((id, entry) -> {
            try {
                entry.session().close(CloseStatus.GOING_AWAY.withReason("Server shutting down"));
            } catch (Exception e) {
                log.log(Level.FINE, "Error closing WS session during shutdown", e);
            } finally {
                sessions.remove(id);
            }
        });
    }
}
