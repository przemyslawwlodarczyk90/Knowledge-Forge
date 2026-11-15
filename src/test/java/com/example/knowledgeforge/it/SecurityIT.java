package com.example.knowledgeforge.it;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends IntegrationTestBase {

    @Test
    void api_requires_authentication_with_json_error() throws Exception {
        mvc.perform(get("/api/categories/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
        mvc.perform(get("/api/categories/tree").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cookie_authenticates_reads_but_never_writes() throws Exception {
        TestUser u = newUser();
        mvc.perform(get("/api/categories/tree").cookie(u.cookie())).andExpect(status().isOk());
        mvc.perform(post("/api/categories").cookie(u.cookie()).contentType("application/json").content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/topics/" + java.util.UUID.randomUUID()).cookie(u.cookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void token_of_deleted_user_is_rejected() throws Exception {
        TestUser u = newUser();
        userRepository.deleteById(u.id());
        mvc.perform(as(u, get("/api/categories/tree"))).andExpect(status().isUnauthorized());
    }

    @Test
    void public_endpoints_and_cors_preflight() throws Exception {
        mvc.perform(get("/api/app-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleBinaryLoader").value(true));
        mvc.perform(options("/api/topics")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void admin_backup_endpoint_is_hidden_when_disabled() throws Exception {
        mvc.perform(get("/api/admin/backups")).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/backups/restore")).andExpect(status().isNotFound());
    }

    @Test
    void spa_routes_fall_back_to_index_html_but_unknown_api_is_404() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(as(newUser(), get("/api/does-not-exist"))).andExpect(status().isNotFound());
    }
}
