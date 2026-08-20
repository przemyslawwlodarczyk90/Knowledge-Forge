package com.example.knowledgeforge.ws;

import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;

/** Mapowany na /ws/updates. Rejestracja wymaga wcześniejszego JettyWebSocketServletContainerInitializer#configure na kontekście (zob. Main.java). */
public class KnowledgeForgeWebSocketServlet extends JettyWebSocketServlet {

    private final WebSocketConnectionRegistry registry;

    public KnowledgeForgeWebSocketServlet(WebSocketConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofSeconds(90));
        factory.setCreator((req, resp) -> new KnowledgeForgeWebSocket(registry));
    }
}
