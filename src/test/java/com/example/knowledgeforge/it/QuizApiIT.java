package com.example.knowledgeforge.it;

import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;
import com.example.knowledgeforge.quiz.QuizBatch;
import com.example.knowledgeforge.quiz.QuizChunk;
import com.example.knowledgeforge.quiz.QuizGenerationPort;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pełny przepływ quizu przez HTTP, Security i prawdziwą bazę (kf_test): zapis notatki -> "Wygeneruj
 * quiz z AI" (model podmieniony) -> pula 100 -> kolejne 100 -> losowanie -> limit dzienny -> czyszczenie
 * -> kaskadowe usunięcie z tematem.
 */
class QuizApiIT extends IntegrationTestBase {

    @MockitoBean QuizGenerationPort port;

    static final AtomicInteger SEQ = new AtomicInteger();

    static QuizBatch answering(QuizChunk c) {
        List<QuizBatch.Item> items = new ArrayList<>();
        for (int i = 0; i < c.count(); i++) {
            items.add(new QuizBatch.Item(c.difficulty(), "Pytanie nr " + SEQ.incrementAndGet() + " o " + c.topicTitle(),
                    i % 2 == 0 ? "int x = " + i + ";\n    return x;" : null,
                    List.of("odp a", "odp b", "odp c", "odp d"), i % 4, "Bo tak."));
        }
        return new QuizBatch(items);
    }

    static Map<String, Object> doc(String text) {
        return Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", text)))));
    }

    String topicWithNote(TestUser u, String title) throws Exception {
        String id = createTopic(u, createCategory(u, "Quiz " + title), title).get("id").asText();
        mvc.perform(withJson(as(u, put("/api/topics/" + id + "/note")), Map.of("contentJson", doc("Treść o " + title))))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void full_flow_generate_grow_pool_draw_limit_clear_and_cascade() throws Exception {
        reset(port);
        when(port.generate(any())).thenAnswer(inv -> answering(inv.getArgument(0)));
        when(port.modelName()).thenReturn("gpt-test");
        TestUser u = newUser();
        String id = topicWithNote(u, "Rekurencja");
        String base = "/api/topics/" + id + "/quiz";

        mvc.perform(as(u, get(base + "/stats"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.passRatio").value(0.75))
                .andExpect(jsonPath("$.questionsPerGeneration").value(100));
        mvc.perform(as(u, get(base))).andExpect(status().isOk()).andExpect(jsonPath("$.questions.length()").value(0));

        mvc.perform(withJson(as(u, post(base + "/generate")), Map.of("instruction", "więcej kodu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(100))
                .andExpect(jsonPath("$.failedChunks").value(0))
                .andExpect(jsonPath("$.stats.total").value(100))
                .andExpect(jsonPath("$.stats.easy").value(30))
                .andExpect(jsonPath("$.stats.medium").value(40))
                .andExpect(jsonPath("$.stats.hard").value(30))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.usedToday").value(1))
                .andExpect(jsonPath("$.dailyLimit").value(2));

        // Drugie kliknięcie dorzuca kolejne 100 — pula rośnie do 200.
        mvc.perform(as(u, post(base + "/generate"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.total").value(200))
                .andExpect(jsonPath("$.usedToday").value(2));
        // Limit dzienny (TestEnv: 2).
        mvc.perform(as(u, post(base + "/generate"))).andExpect(status().isTooManyRequests());

        // Każde podejście losuje na nowo z całej puli.
        JsonNode first = body(mvc.perform(as(u, get(base).param("count", "20"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(20))
                .andExpect(jsonPath("$.poolSize").value(200))
                .andExpect(jsonPath("$.passRatio").value(0.75))
                .andReturn());
        JsonNode q = first.get("questions").get(0);
        assertThat(q.get("options")).hasSize(4);
        assertThat(q.get("correctIndex").asInt()).isBetween(0, 3);
        assertThat(q.get("explanation").asText()).isEqualTo("Bo tak.");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            body(mvc.perform(as(u, get(base).param("count", "20"))).andReturn())
                    .get("questions").forEach(n -> seen.add(n.get("id").asText()));
        }
        assertThat(seen.size()).isGreaterThan(20);
        boolean hasCode = false;
        for (JsonNode n : first.get("questions")) hasCode |= n.hasNonNull("code") && n.get("code").asText().contains("\n    return x;");
        assertThat(hasCode).isTrue();

        mvc.perform(as(u, get(base).param("count", "0"))).andExpect(status().isBadRequest());
        mvc.perform(as(u, get(base).param("count", "99"))).andExpect(status().isBadRequest());

        mvc.perform(as(u, delete(base))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        // Kaskada: pytania znikają razem z tematem.
        TestUser v = newUser();
        String other = topicWithNote(v, "Grafy");
        mvc.perform(as(v, post("/api/topics/" + other + "/quiz/generate"))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quiz_question WHERE topic_id = ?", Integer.class,
                UUID.fromString(other))).isEqualTo(100);
        mvc.perform(as(v, delete("/api/topics/" + other))).andExpect(status().is2xxSuccessful());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quiz_question WHERE topic_id = ?", Integer.class,
                UUID.fromString(other))).isZero();
    }

    @Test
    void errors_ownership_missing_note_and_ai_not_configured() throws Exception {
        reset(port);
        TestUser u = newUser();
        String noNote = createTopic(u, createCategory(u, "Quiz"), "Pusty").get("id").asText();
        mvc.perform(as(u, post("/api/topics/" + noNote + "/quiz/generate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Najpierw zapisz treść notatki — quiz powstaje na jej podstawie."));

        String id = topicWithNote(u, "Sortowanie");
        doThrow(new AiNotConfiguredException("brak klucza")).when(port).generate(any());
        mvc.perform(as(u, post("/api/topics/" + id + "/quiz/generate")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("brak klucza"));

        mvc.perform(withJson(as(u, post("/api/topics/" + id + "/quiz/generate")), Map.of("instruction", "x".repeat(1001))))
                .andExpect(status().isBadRequest());

        TestUser stranger = newUser();
        mvc.perform(as(stranger, get("/api/topics/" + id + "/quiz/stats"))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, get("/api/topics/" + id + "/quiz"))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, delete("/api/topics/" + id + "/quiz"))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, post("/api/topics/" + id + "/quiz/generate"))).andExpect(status().isNotFound());
        mvc.perform(as(u, get("/api/topics/zle-id/quiz/stats"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/topics/" + id + "/quiz/stats")).andExpect(status().isUnauthorized());
    }
}
