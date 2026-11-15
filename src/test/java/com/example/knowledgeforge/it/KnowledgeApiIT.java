package com.example.knowledgeforge.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeApiIT extends IntegrationTestBase {

    private static Map<String, Object> doc(String text) {
        return Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", text))),
                Map.of("type", "image", "attrs", Map.of("src", "asset:img1.png"))));
    }

    private static List<Map<String, String>> png() {
        return List.of(Map.of("id", "img1.png",
                "dataBase64", Base64.getEncoder().encodeToString("\u0089PNG fake".getBytes())));
    }

    @Test
    void category_tree_crud_and_rules() throws Exception {
        TestUser u = newUser();
        mvc.perform(as(u, get("/api/categories/tree"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.children", hasSize(0)));

        UUID parent = createCategory(u, "Programowanie");
        var child = new HashMap<String, Object>(Map.of("name", "Java", "parentId", parent));
        UUID childId = UUID.fromString(body(mvc.perform(withJson(as(u, post("/api/categories")), child))
                .andExpect(status().isCreated()).andReturn()).get("id").asText());

        mvc.perform(withJson(as(u, post("/api/categories")), Map.of("name", "programowanie")))
                .andExpect(status().isConflict());
        mvc.perform(withJson(as(u, post("/api/categories")), Map.of("name", " ")))
                .andExpect(status().isBadRequest());

        mvc.perform(withJson(as(u, patch("/api/categories/" + childId)), Map.of("name", "Java 21")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Java 21"));
        mvc.perform(as(u, delete("/api/categories/" + parent))).andExpect(status().isConflict());
        mvc.perform(as(u, delete("/api/categories/" + childId))).andExpect(status().isNoContent());
        mvc.perform(as(u, delete("/api/categories/" + parent))).andExpect(status().isNoContent());
        mvc.perform(as(u, patch("/api/categories/" + UUID.randomUUID())).contentType("application/json").content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void topic_lifecycle_with_note_search_filters_and_optimistic_locking() throws Exception {
        TestUser u = newUser();
        UUID cat = createCategory(u, "Kat");
        JsonNode topic = createTopic(u, cat, "Rekurencja");
        String id = topic.get("id").asText();
        assertThat(topic.get("categoryTopicCount").asInt()).isEqualTo(1);

        mvc.perform(as(u, get("/api/topics/" + id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rekurencja"));
        mvc.perform(as(u, get("/api/categories/" + cat + "/topics"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(u, get("/api/topics/xyz"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid id: xyz"));
        mvc.perform(as(u, get("/api/topics/" + UUID.randomUUID()))).andExpect(status().isNotFound());

        // notatka: zapis, odczyt, obrazek, konflikt wersji
        mvc.perform(withJson(as(u, put("/api/topics/" + id + "/note")), Map.of("contentJson", doc("slowoKluczowe w tresci"), "assets", png())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        JsonNode note = body(mvc.perform(as(u, get("/api/topics/" + id + "/note"))).andExpect(status().isOk()).andReturn());
        String src = note.get("contentJson").get("content").get(1).get("attrs").get("src").asText();
        assertThat(src).startsWith("/api/topics/" + id + "/note/assets/");
        mvc.perform(get(src).cookie(u.cookie())).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(withJson(as(u, put("/api/topics/" + id + "/note")), Map.of("contentJson", doc("v2"))))
                .andExpect(status().isBadRequest());
        mvc.perform(withJson(as(u, put("/api/topics/" + id + "/note")), Map.of("contentJson", doc("v2"), "baseVersion", 7)))
                .andExpect(status().isConflict());
        mvc.perform(withJson(as(u, put("/api/topics/" + id + "/note")),
                        Map.of("contentJson", doc("slowoKluczowe v2"), "baseVersion", 1, "assets", png())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));

        JsonNode afterNote = body(mvc.perform(as(u, get("/api/topics/" + id))).andReturn());
        assertThat(afterNote.get("status").asText()).isEqualTo("NOTE_ADDED");

        // PATCH z optimistic lockingiem
        int v = afterNote.get("version").asInt();
        mvc.perform(withJson(as(u, patch("/api/topics/" + id)), Map.of("title", "Rekurencja 2", "author", "Ala", "expectedVersion", v)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(v + 1));
        mvc.perform(withJson(as(u, patch("/api/topics/" + id)), Map.of("title", "x", "expectedVersion", v)))
                .andExpect(status().isConflict());

        // wyszukiwanie: tytuł, opis, treść
        mvc.perform(as(u, get("/api/search").param("q", "rekurencja 2"))).andExpect(jsonPath("$[0].matchedIn").value("TITLE"));
        mvc.perform(as(u, get("/api/search").param("q", "slowokluczowe"))).andExpect(jsonPath("$[0].matchedIn").value("CONTENT"));
        mvc.perform(as(u, get("/api/search").param("q", "opis Rekurencja"))).andExpect(jsonPath("$[0].matchedIn").value("DESCRIPTION"));
        mvc.perform(as(u, get("/api/search").param("q", " "))).andExpect(jsonPath("$", hasSize(0)));

        // filtry
        mvc.perform(as(u, get("/api/topics").param("author", "ala"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(u, get("/api/topics").param("detailLevel", "MEDIUM").param("categoryId", cat.toString())))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(u, get("/api/topics"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(u, get("/api/topics").param("detailLevel", "NOPE"))).andExpect(status().isBadRequest());
        mvc.perform(as(u, get("/api/topics/authors"))).andExpect(jsonPath("$[0]").value("Ala"));

        // usunięcie tematu z notatką
        mvc.perform(as(u, delete("/api/topics/" + id))).andExpect(status().isNoContent());
        mvc.perform(as(u, get("/api/topics/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void actuality_review_and_manual_confirmation() throws Exception {
        TestUser u = newUser();
        UUID cat = createCategory(u, "Kat");
        String id = createTopic(u, cat, "Stary temat").get("id").asText();
        jdbc.update("UPDATE topic SET actuality_verified = FALSE WHERE id = ?::uuid", id);

        mvc.perform(as(u, get("/api/topics/actuality-review"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(u, get("/api/topics/actuality-review").param("author", "Nikt"))).andExpect(jsonPath("$", hasSize(0)));

        int v = body(mvc.perform(as(u, get("/api/topics/" + id))).andReturn()).get("version").asInt();
        mvc.perform(withJson(as(u, post("/api/topics/" + id + "/verify-actuality")), Map.of("expectedVersion", v + 5)))
                .andExpect(status().isConflict());
        mvc.perform(withJson(as(u, post("/api/topics/" + id + "/verify-actuality")), Map.of("expectedVersion", v)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.actualityVerified").value(true));
        mvc.perform(as(u, get("/api/topics/actuality-review"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void attachments_upload_stream_range_download_and_delete() throws Exception {
        TestUser u = newUser();
        UUID cat = createCategory(u, "Kat");
        String topicId = createTopic(u, cat, "Z filmem").get("id").asText();
        byte[] mp4 = new byte[2048];
        System.arraycopy(new byte[]{0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'}, 0, mp4, 0, 12);

        JsonNode att = body(mvc.perform(as(u, multipart("/api/topics/" + topicId + "/attachments")
                        .part(new MockPart("file", "film.mp4", mp4, MediaType.parseMediaType("video/mp4")))
                        .part(new MockPart("description", "opis".getBytes()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachmentType").value("VIDEO"))
                .andReturn());
        long attId = att.get("id").asLong();

        mvc.perform(as(u, multipart("/api/topics/" + topicId + "/attachments")
                        .part(new MockPart("file", "a.txt", new byte[0], MediaType.TEXT_PLAIN))))
                .andExpect(status().isBadRequest());

        mvc.perform(as(u, get("/api/topics/" + topicId + "/attachments"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(u, get("/api/attachments/" + attId))).andExpect(jsonPath("$.originalName").value("film.mp4"));
        mvc.perform(get("/api/attachments/" + attId + "/view").cookie(u.cookie()).header("Range", "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-99/2048"));
        mvc.perform(get("/api/attachments/" + attId + "/view").cookie(u.cookie()))
                .andExpect(status().isOk()).andExpect(header().string("Accept-Ranges", "bytes"));
        mvc.perform(get("/api/attachments/" + attId + "/view").cookie(u.cookie()).header("Range", "bytes=5000-6000"))
                .andExpect(status().isRequestedRangeNotSatisfiable());
        mvc.perform(get("/api/attachments/" + attId + "/download").cookie(u.cookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"film.mp4\"")));

        TestUser stranger = newUser();
        mvc.perform(get("/api/attachments/" + attId + "/download").cookie(stranger.cookie())).andExpect(status().isNotFound());
        mvc.perform(as(u, get("/api/attachments/abc"))).andExpect(status().isBadRequest());

        mvc.perform(as(u, delete("/api/attachments/" + attId))).andExpect(status().isNoContent());
        mvc.perform(as(u, get("/api/attachments/" + attId))).andExpect(status().isNotFound());
    }

    @Test
    void users_never_see_each_others_data() throws Exception {
        TestUser owner = newUser();
        TestUser stranger = newUser();
        UUID cat = createCategory(owner, "Prywatne");
        String id = createTopic(owner, cat, "Sekretny temat").get("id").asText();

        mvc.perform(as(stranger, get("/api/topics/" + id))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, get("/api/categories/" + cat + "/topics"))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, get("/api/search").param("q", "Sekretny"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(stranger, delete("/api/topics/" + id))).andExpect(status().isNotFound());
        mvc.perform(withJson(as(stranger, patch("/api/topics/" + id)), Map.of("title", "x"))).andExpect(status().isNotFound());
    }
}
