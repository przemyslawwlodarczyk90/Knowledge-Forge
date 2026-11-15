package com.example.knowledgeforge.service;

import com.example.knowledgeforge.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Id zalogowanego użytkownika bieżącego żądania — z principala ustawionego przez JwtAuthFilter.
 * Wszystkie dane (kategorie, tematy, notatki, załączniki) są filtrowane po tym id, więc każdy
 * użytkownik widzi wyłącznie swoje.
 */
@Component
public class CurrentUser {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id();
        }
        throw new IllegalStateException("No authenticated user in the current request");
    }
}
