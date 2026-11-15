package com.example.knowledgeforge.it;

import com.example.knowledgeforge.domain.user.ConfirmationToken;
import com.example.knowledgeforge.domain.user.Role;
import com.example.knowledgeforge.repository.ConfirmationTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIT extends IntegrationTestBase {

    @Autowired ConfirmationTokenRepository tokenRepository;

    private String uniqueName() {
        return "reg-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void register_login_me_and_logout_round_trip() throws Exception {
        String name = uniqueName();
        mvc.perform(withJson(post("/api/auth/register"), Map.of("username", name, "email", name + "@x.pl", "password", "Secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        var login = mvc.perform(withJson(post("/api/auth/login"), Map.of("username", name, "password", "Secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().httpOnly("KF_TOKEN", true))
                .andReturn();
        String token = body(login).get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));

        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("KF_TOKEN", 0));
    }

    @Test
    void registration_creates_inactive_account_and_activation_token() throws Exception {
        String name = uniqueName();
        mvc.perform(withJson(post("/api/auth/register"), Map.of("username", name, "email", name + "@x.pl", "password", "Secret123")))
                .andExpect(status().isCreated());
        var user = userRepository.findByUsername(name).orElseThrow();
        assertThat(user.isActive()).isFalse();
        assertThat(passwordEncoder.matches("Secret123", user.getPassword())).isTrue();
        Long tokens = jdbc.queryForObject("SELECT COUNT(*) FROM confirmation_tokens WHERE user_id = ?", Long.class, user.getId());
        assertThat(tokens).isEqualTo(1);
    }

    @Test
    void duplicate_username_and_invalid_payload_are_rejected() throws Exception {
        String name = uniqueName();
        var body = Map.of("username", name, "email", name + "@x.pl", "password", "Secret123");
        mvc.perform(withJson(post("/api/auth/register"), body)).andExpect(status().isCreated());
        mvc.perform(withJson(post("/api/auth/register"), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already taken"));

        mvc.perform(withJson(post("/api/auth/register"), Map.of("username", "ab", "email", "nope", "password", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void wrong_password_gives_401_with_message() throws Exception {
        TestUser u = newUser();
        mvc.perform(withJson(post("/api/auth/login"), Map.of("username", u.username(), "password", "bad")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void confirm_registration_activates_account_and_handles_bad_and_expired_tokens() throws Exception {
        String name = uniqueName();
        mvc.perform(withJson(post("/api/auth/register"), Map.of("username", name, "email", name + "@x.pl", "password", "Secret123")))
                .andExpect(status().isCreated());
        Long userId = userRepository.findByUsername(name).orElseThrow().getId();
        String token = jdbc.queryForObject("SELECT token FROM confirmation_tokens WHERE user_id = ?", String.class, userId);

        mvc.perform(get("/api/v1/registration/confirm").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration confirmed!"));
        assertThat(userRepository.findById(userId).orElseThrow().isActive()).isTrue();

        mvc.perform(get("/api/v1/registration/confirm").param("token", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid token"));

        String expired = UUID.randomUUID().toString();
        tokenRepository.save(new ConfirmationToken(expired, LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2), userId));
        mvc.perform(get("/api/v1/registration/confirm").param("token", expired))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    void profile_update_and_password_change_apply_to_current_user_only() throws Exception {
        TestUser u = newUser();
        TestUser other = newUser();
        String newEmail = "new-" + u.username() + "@x.pl";
        mvc.perform(withJson(as(u, put("/api/auth/update")), Map.of("email", newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        mvc.perform(withJson(as(u, put("/api/auth/update")), Map.of("username", other.username())))
                .andExpect(status().isConflict());

        String renamed = u.username() + "-r";
        mvc.perform(withJson(as(u, put("/api/auth/update")), Map.of("username", renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(renamed));
        // token jest związany z id, więc zmiana nazwy nie wylogowuje
        mvc.perform(as(u, get("/api/auth/me"))).andExpect(status().isOk());

        mvc.perform(withJson(as(u, post("/api/auth/change-password")), Map.of("currentPassword", "bad", "newPassword", "Other123")))
                .andExpect(status().isBadRequest());
        mvc.perform(withJson(as(u, post("/api/auth/change-password")), Map.of("currentPassword", "Secret123", "newPassword", "Other123")))
                .andExpect(status().isOk());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("username", renamed, "password", "Other123")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_endpoint_requires_admin_role_and_seeded_admin_can_log_in() throws Exception {
        mvc.perform(get("/api/admin/status")).andExpect(status().isUnauthorized());
        mvc.perform(as(newUser(), get("/api/admin/status"))).andExpect(status().isForbidden());
        mvc.perform(as(newUser(Role.ADMIN), get("/api/admin/status")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ready"));

        JsonNode admin = body(mvc.perform(withJson(post("/api/auth/login"),
                Map.of("username", "it-admin", "password", "ItAdmin123!"))).andExpect(status().isOk()).andReturn());
        assertThat(admin.get("role").asText()).isEqualTo("ADMIN");
    }
}
