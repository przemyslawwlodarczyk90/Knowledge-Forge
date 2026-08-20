package com.example.knowledgeforge.ws;

import org.eclipse.jetty.websocket.api.Session;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// MULTI-THREADING:
// Jetty obsługuje otwarcia/zamknięcia i błędy WebSocket na różnych wątkach z własnej puli
// I/O (nie na wątku żądania HTTP, który wywołuje broadcast() po commicie transakcji).
// ConcurrentHashMap.newKeySet() daje bezpieczny współbieżnie zbiór sesji BEZ globalnego locka —
// dodanie/usunięcie jednej sesji nie blokuje ani innych sesji, ani trwającego broadcastu.
// Iteracja w broadcast() korzysta z "weakly consistent" iteratora ConcurrentHashMap: nie rzuci
// ConcurrentModificationException nawet jeśli w trakcie wysyłki jakaś sesja się zamknie i zniknie
// z zbioru na innym wątku. Uszkodzona/zamknięta sesja jest usuwana od razu (remove()), a wysyłka
// do pozostałych klientów leci dalej — jeden zerwany klient nie przerywa broadcastu.
public class WebSocketConnectionRegistry {

    private static final Logger log = Logger.getLogger(WebSocketConnectionRegistry.class.getName());

    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public void add(Session session) {
        sessions.add(session);
        log.fine(() -> "WS session opened: " + session.getRemoteAddress() + " (active=" + sessions.size() + ")");
    }

    public void remove(Session session) {
        boolean removed = sessions.remove(session);
        if (removed) {
            log.fine(() -> "WS session removed (active=" + sessions.size() + ")");
        }
    }

    public int activeCount() {
        return sessions.size();
    }

    /**
     * Rozsyła tekstową wiadomość do wszystkich aktywnych sesji. Błąd wysyłki do JEDNEGO klienta
     * (zerwane łącze, zamknięty socket) jest łapany per-sesja i nie przerywa wysyłki do reszty —
     * uszkodzona sesja jest od razu usuwana z rejestru.
     */
    public void broadcast(String json) {
        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(json);
                } else {
                    sessions.remove(session);
                }
            } catch (Exception e) {
                log.log(Level.FINE, "WS broadcast: failed to send to one session, removing it", e);
                sessions.remove(session);
            }
        }
    }

    /** Wysyła ping (kontrola aktywności) do wszystkich sesji — wołane przez jeden współdzielony scheduler (zob. WebSocketHeartbeat). */
    public void pingAll() {
        ByteBuffer payload = ByteBuffer.wrap(new byte[0]);
        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendPing(payload.duplicate());
                } else {
                    sessions.remove(session);
                }
            } catch (Exception e) {
                log.log(Level.FINE, "WS heartbeat: failed to ping one session, removing it", e);
                sessions.remove(session);
            }
        }
    }

    /** Wołane przy zamykaniu aplikacji — zamyka wszystkie sesje w kontrolowany sposób. */
    public void closeAll() {
        for (Session session : sessions) {
            try {
                session.close(1001, "Server shutting down");
            } catch (Exception e) {
                log.log(Level.FINE, "Error closing WS session during shutdown", e);
            } finally {
                sessions.remove(session);
            }
        }
    }
}
