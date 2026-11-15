package com.example.knowledgeforge.ws;

import com.example.knowledgeforge.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Handshake /ws/updates przechodzi przez łańcuch Spring Security (JWT z ciasteczka KF_TOKEN —
 * przeglądarkowy WebSocket nie pozwala ustawić nagłówka Authorization). Tutaj tylko przepisujemy
 * id zalogowanego użytkownika do atrybutów sesji; bez uwierzytelnienia handshake jest odrzucany.
 */
public class AuthenticatedHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "kf.userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            attributes.put(USER_ID_ATTRIBUTE, user.id());
            return true;
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
