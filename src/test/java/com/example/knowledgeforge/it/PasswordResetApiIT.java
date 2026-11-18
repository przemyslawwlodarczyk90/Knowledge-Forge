package com.example.knowledgeforge.it;

import com.example.knowledgeforge.auth.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetApiIT extends IntegrationTestBase {

    @MockitoBean MailService mail;

    @Test
    void sendsLinkToAccountEmailAndAllowsItToBeUsedOnlyOnce() throws Exception {
        TestUser user = newUser();

        requestReset("missing-" + UUID.randomUUID());
        requestReset(user.username());
        requestReset(user.username());

        var link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordResetEmail(eq(user.username() + "@test.local"), link.capture());
        URI uri = URI.create(link.getValue());
        assertThat(uri.getPath()).isEqualTo("/reset-password");
        assertThat(uri.getQuery()).isNull();
        String token = uri.getFragment().substring("token=".length());

        var newPassword = Map.of("token", token, "password", "NewSecret123");
        mvc.perform(withJson(post("/api/auth/reset-password"), newPassword)).andExpect(status().isNoContent());
        mvc.perform(withJson(post("/api/auth/reset-password"), newPassword)).andExpect(status().isBadRequest());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("username", user.username(), "password", "Secret123")))
                .andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("username", user.username(), "password", "NewSecret123")))
                .andExpect(status().isOk());
    }

    private void requestReset(String username) throws Exception {
        mvc.perform(withJson(post("/api/auth/forgot-password"), Map.of("username", username)))
                .andExpect(status().isAccepted());
    }
}
