package com.example.knowledgeforge.it;

import com.example.knowledgeforge.domain.user.Role;
import com.example.knowledgeforge.domain.user.User;
import com.example.knowledgeforge.repository.UserRepository;
import com.example.knowledgeforge.security.AuthCookies;
import com.example.knowledgeforge.security.JwtService;
import com.example.knowledgeforge.support.TestDatabase;
import com.example.knowledgeforge.support.TestEnv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pełny kontekst aplikacji (Security + MVC + JPA + WebSocket) na schemacie testowym kf_test.
 * Konfiguracja wyłącznie z TestEnv (DynamicPropertySource ma pierwszeństwo przed .env), schedulery
 * wyłączone, pliki w katalogu tymczasowym. Kontekst jest współdzielony między klasami testów.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    static final Path TEMP_DIR;

    static {
        try {
            TEMP_DIR = Files.createTempDirectory("kf-it-");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void testEnv(DynamicPropertyRegistry registry) {
        TestEnv.values(TEMP_DIR).forEach((k, v) -> registry.add(k, () -> v));
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    /** Twardy bezpiecznik: nigdy nie testujemy na roboczym schemacie (public). */
    @BeforeEach
    void guardSchema() {
        assertEquals(TestDatabase.SCHEMA, jdbc.queryForObject("SELECT current_schema()", String.class),
                "integration tests must run against the test schema only");
    }

    /** Zalogowany użytkownik testowy: konto w bazie + token JWT. */
    protected record TestUser(Long id, String username, String token) {
        public String bearer() {
            return "Bearer " + token;
        }

        public Cookie cookie() {
            return new Cookie(AuthCookies.NAME, token);
        }
    }

    protected TestUser newUser(Role role) {
        User u = new User();
        u.setUsername("it-" + role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8));
        u.setEmail(u.getUsername() + "@test.local");
        u.setPassword(passwordEncoder.encode("Secret123"));
        u.setActive(true);
        u.setRole(role);
        userRepository.save(u);
        return new TestUser(u.getId(), u.getUsername(), jwtService.generateToken(u));
    }

    protected TestUser newUser() {
        return newUser(Role.USER);
    }

    protected MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder req) {
        return req.header("Authorization", user.bearer()).header("X-Client-Id", "it-client");
    }

    protected MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected UUID createCategory(TestUser user, String name) throws Exception {
        MvcResult r = mvc.perform(withJson(as(user, post("/api/categories")), java.util.Map.of("name", name)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(body(r).get("id").asText());
    }

    protected JsonNode createTopic(TestUser user, UUID categoryId, String title) throws Exception {
        var req = new java.util.HashMap<String, Object>();
        req.put("categoryId", categoryId);
        req.put("title", title);
        req.put("shortPrompt", "opis " + title);
        req.put("author", "Autor");
        req.put("detailLevel", "MEDIUM");
        req.put("type", "NOTE");
        MvcResult r = mvc.perform(withJson(as(user, post("/api/topics")), req))
                .andExpect(status().isCreated()).andReturn();
        return body(r);
    }
}
