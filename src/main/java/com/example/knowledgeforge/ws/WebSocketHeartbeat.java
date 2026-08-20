package com.example.knowledgeforge.ws;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WebSocketHeartbeat {

    private static final Logger log = Logger.getLogger(WebSocketHeartbeat.class.getName());

    private final WebSocketConnectionRegistry registry;
    private ScheduledExecutorService scheduler;

    public WebSocketHeartbeat(WebSocketConnectionRegistry registry) {
        this.registry = registry;
    }

    // MULTI-THREADING:
    // Jeden współdzielony ScheduledExecutorService (pojedynczy wątek-demon) obsługuje heartbeat
    // WSZYSTKICH aktywnych połączeń naraz — registry.pingAll() w jednym przebiegu iteruje cały
    // zbiór sesji i wysyła ping do każdej. Nie tworzymy osobnego wątku/timera per klient, więc
    // liczba jednocześnie otwartych kart przeglądarki nie powoduje wzrostu liczby wątków
    // aplikacji. Zadanie jest krótkie (tylko wysyłka ramek ping, bez oczekiwania na pong),
    // więc nie ma ryzyka zablokowania kolejnych tików schedulera.
    public void start(Duration interval) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-heartbeat");
            t.setDaemon(true);
            return t;
        });
        long seconds = Math.max(1, interval.toSeconds());
        scheduler.scheduleAtFixedRate(registry::pingAll, seconds, seconds, TimeUnit.SECONDS);
        log.info(() -> "WebSocket heartbeat started (every " + seconds + "s)");
    }

    public void shutdown() {
        if (scheduler == null) return;
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
