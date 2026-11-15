package com.example.knowledgeforge.it;

import com.example.knowledgeforge.ai.NoteContent;
import com.example.knowledgeforge.ai.NoteDifficulty;
import com.example.knowledgeforge.ai.NoteGenerationPort;
import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;
import com.example.knowledgeforge.domain.exception.AiResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoint generowania notatki — model OpenAI podmieniony mockiem portu (bez prawdziwych wywołań API). */
class NoteGenerationIT extends IntegrationTestBase {

    @MockitoBean NoteGenerationPort port;

    static NoteContent sample() {
        NoteContent c = new NoteContent();
        c.setTitle("Rekurencja");
        c.setSummary("Funkcja wywołuje samą siebie.");
        NoteContent.Section s = new NoteContent.Section();
        s.setHeading("Warunek stopu");
        s.setContent("Bez niego stos się przepełni.");
        c.setSections(List.of(s));
        c.setExamples(List.of(new NoteContent.Example(), new NoteContent.Example(), new NoteContent.Example()));
        c.setMemoryPoints(List.of("Zawsze warunek stopu"));
        return c;
    }

    @Test
    void generates_editor_document_uses_topic_defaults_and_enforces_daily_limit() throws Exception {
        TestUser u = newUser();
        String id = createTopic(u, createCategory(u, "AI"), "Rekurencja").get("id").asText();
        when(port.generate(any(), any(), any())).thenReturn(sample());
        when(port.modelName()).thenReturn("gpt-test");

        mvc.perform(as(u, post("/api/topics/" + id + "/note/generate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rekurencja"))
                .andExpect(jsonPath("$.contentJson.type").value("doc"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.usedToday").value(1))
                .andExpect(jsonPath("$.dailyLimit").value(3));
        verify(port).generate(eq("Rekurencja"), eq("opis Rekurencja"), eq(NoteDifficulty.MEDIUM));

        mvc.perform(withJson(as(u, post("/api/topics/" + id + "/note/generate")), Map.of("instruction", "krócej", "difficulty", "HARD")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.difficulty").value("HARD"));
        verify(port).generate(eq("Rekurencja"), eq("krócej"), eq(NoteDifficulty.HARD));

        mvc.perform(as(u, post("/api/topics/" + id + "/note/generate"))).andExpect(status().isOk());
        mvc.perform(as(u, post("/api/topics/" + id + "/note/generate")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void maps_ai_failures_to_http_statuses_and_checks_ownership() throws Exception {
        TestUser u = newUser();
        String id = createTopic(u, createCategory(u, "AI"), "Temat").get("id").asText();

        doThrow(new AiNotConfiguredException("brak klucza")).when(port).generate(any(), any(), any());
        mvc.perform(as(u, post("/api/topics/" + id + "/note/generate")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("brak klucza"));

        doThrow(new AiResponseException("zła odpowiedź")).when(port).generate(any(), any(), any());
        mvc.perform(as(u, post("/api/topics/" + id + "/note/generate"))).andExpect(status().isBadGateway());

        mvc.perform(as(newUser(), post("/api/topics/" + id + "/note/generate"))).andExpect(status().isNotFound());
        mvc.perform(as(u, post("/api/topics/" + UUID.randomUUID() + "/note/generate"))).andExpect(status().isNotFound());
        mvc.perform(as(u, post("/api/topics/zle-id/note/generate"))).andExpect(status().isBadRequest());
    }
}
