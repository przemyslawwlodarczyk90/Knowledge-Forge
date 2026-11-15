package com.example.knowledgeforge.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.knowledgeforge.domain.user.User;
import com.example.knowledgeforge.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Uwierzytelnia żądanie na podstawie JWT: nagłówek "Authorization: Bearer ..." (zawsze), albo
 * ciasteczko KF_TOKEN — ale TYLKO dla GET/HEAD (zob. AuthCookies). Konto jest doczytywane z bazy
 * przy każdym żądaniu, więc usunięte/zablokowane konto natychmiast traci dostęp.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final boolean requireActiveAccount;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, boolean requireActiveAccount) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.requireActiveAccount = requireActiveAccount;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                    .flatMap(jwtService::verify)
                    .flatMap(this::loadUser)
                    .ifPresent(user -> {
                        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
                        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        String method = request.getMethod();
        if (("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) && request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (AuthCookies.NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return Optional.of(c.getValue());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<User> loadUser(DecodedJWT jwt) {
        Long uid = jwt.getClaim("uid").asLong();
        Optional<User> user = uid != null
                ? userRepository.findById(uid)
                : userRepository.findByUsername(jwt.getSubject());
        // uid jest podpisany razem z tokenem — zmiana nazwy użytkownika nie unieważnia sesji.
        return user.filter(u -> !requireActiveAccount || u.isActive());
    }
}
