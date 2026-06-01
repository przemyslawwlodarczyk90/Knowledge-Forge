package com.example.offerbrowserprototype.infrastructure.security;

import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String subject = jwtService.extractUsername(token); // to jest username (sub)

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Próbuj po username, fallback po email (JWT sub może być emailem)
                UserDetails userDetails = null;
                try {
                    userDetails = userDetailsService.loadUserByUsername(subject);
                } catch (Exception e) {
                    try {
                        String usernameByEmail = userRepository.findByEmail(subject)
                                .map(u -> u.getUsername())
                                .orElse(null);
                        if (usernameByEmail != null) {
                            userDetails = userDetailsService.loadUserByUsername(usernameByEmail);
                        } else {
                            log.warn("Could not load user by username or email '{}'", subject);
                        }
                    } catch (Exception ex) {
                        log.warn("Could not load user by email '{}': {}", subject, ex.getMessage());
                    }
                }

                if (userDetails != null && jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("JWT processing error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}