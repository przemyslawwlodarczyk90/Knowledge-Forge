package com.example.knowledgeforge.ws;

import com.example.knowledgeforge.config.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.time.Duration;

/** Rejestruje /ws/updates (surowy WebSocket, bez STOMP/SockJS — protokół dla frontendu bez zmian). */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final KnowledgeForgeWebSocketHandler handler;
    private final AppConfig config;

    public WebSocketConfig(KnowledgeForgeWebSocketHandler handler, AppConfig config) {
        this.handler = handler;
        this.config = config;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/updates")
                .addInterceptors(new AuthenticatedHandshakeInterceptor())
                // Ten sam origin jest zawsze dozwolony; dodatkowo frontend dev (cors.allowed-origin).
                .setAllowedOrigins(config.corsAllowedOrigin());
    }

    /** Odpowiednik dawnego factory.setIdleTimeout(90s) — heartbeat co 25s utrzymuje żywe połączenia. */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(Duration.ofSeconds(90).toMillis());
        return container;
    }

    @Bean(initMethod = "startDefault", destroyMethod = "shutdown")
    public WebSocketHeartbeat webSocketHeartbeat(WebSocketConnectionRegistry registry) {
        return new WebSocketHeartbeat(registry);
    }
}
