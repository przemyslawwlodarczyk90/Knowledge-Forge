package com.example.knowledgeforge.security;

import com.example.knowledgeforge.domain.user.Role;

/** Principal w SecurityContext — wypełniany przez JwtAuthFilter na podstawie tokenu i rekordu users. */
public record AuthenticatedUser(Long id, String username, Role role) {
}
