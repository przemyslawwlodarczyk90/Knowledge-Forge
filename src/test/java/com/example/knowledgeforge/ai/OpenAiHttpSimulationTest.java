package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.AiResponseException;
import com.example.knowledgeforge.json.JsonMapper;
import com.example.knowledgeforge.quiz.QuizBatch;
import com.example.knowledgeforge.quiz.QuizChunk;
import com.example.knowledgeforge.quiz.QuizDifficulty;
import com.example.knowledgeforge.quiz.QuizGenerationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Symulowane wywołania OpenAI na poziomie HTTP: lokalny serwer udaje /v1/chat/completions, a
 * aplikacja łączy się z nim przez PRAWDZIWE beany z AiConfig (OpenAiApi -> OpenAiChatModel ->
 * ChatClient). Sprawdza nagłówek z kluczem, model, treść promptów i parsowanie odpowiedzi — czyli
 * wszystko, co się wydarzy z prawdziwym kluczem, poza samym modelem.
 */
class OpenAiHttpSimulationTest {

    record Call(String path, String authorization, JsonNode body) {
    }

    HttpServer server;
    final List<Call> calls = new CopyOnWriteArrayList<>();
    volatile Function<JsonNode, Reply> responder;

    record Reply(int status, String body) {
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    void handle(HttpExchange ex) throws IOException {
        JsonNode body = JsonMapper.get().readTree(ex.getRequestBody().readAllBytes());
        calls.add(new Call(ex.getRequestURI().getPath(), ex.getRequestHeaders().getFirst("Authorization"), body));
        Reply reply = responder.apply(body);
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(reply.status(), bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    AppConfig config() {
        Properties p = new Properties();
        p.setProperty("openai.api-key", "sk-simulated");
        p.setProperty("openai.base-url", "http://127.0.0.1:" + server.getAddress().getPort());
        p.setProperty("openai.model", "gpt-4o-mini");
        p.setProperty("openai.temperature", "0.3");
        return AppConfig.fromProperties(p);
    }

    /** Odpowiedź w formacie OpenAI Chat Completions z podaną treścią wiadomości asystenta. */
    static Reply completion(String content) {
        ObjectNode root = JsonMapper.get().createObjectNode()
                .put("id", "chatcmpl-sim").put("object", "chat.completion").put("created", 1_700_000_000)
                .put("model", "gpt-4o-mini");
        ObjectNode choice = root.putArray("choices").addObject().put("index", 0).put("finish_reason", "stop");
        choice.putObject("message").put("role", "assistant").put("content", content);
        root.putObject("usage").put("prompt_tokens", 10).put("completion_tokens", 20).put("total_tokens", 30);
        return new Reply(200, root.toString());
    }

    static String allMessages(JsonNode body) {
        StringBuilder sb = new StringBuilder();
        body.path("messages").forEach(m -> sb.append(m.path("role").asText()).append(": ")
                .append(m.path("content").asText()).append('\n'));
        return sb.toString();
    }

    @Test
    void quiz_port_talks_to_openai_chat_completions_and_parses_questions() {
        responder = body -> completion("""
                {"questions":[{"difficulty":"MEDIUM","question":"Co zwróci list.stream().filter(x -> x > 1).count() dla [1,2,3]?",
                  "code":"List.of(1, 2, 3).stream()\\n    .filter(x -> x > 1)\\n    .count();",
                  "options":["2","3","1","0"],"correctIndex":0,"explanation":"filter zostawia 2 i 3."}]}
                """);
        QuizGenerationPort port = new AiConfig().quizGenerationPort(config());

        QuizBatch batch = port.generate(new QuizChunk("Streamy", "filter i count", null, QuizDifficulty.MEDIUM,
                20, "analiza przykładu", List.of("Czym jest stream?"), 2, 6));

        assertThat(batch.questions()).hasSize(1);
        assertThat(batch.questions().get(0).code()).contains("\n    .filter(x -> x > 1)");
        assertThat(calls).hasSize(1);
        Call call = calls.get(0);
        assertThat(call.path()).isEqualTo("/v1/chat/completions");
        assertThat(call.authorization()).isEqualTo("Bearer sk-simulated");
        assertThat(call.body().path("model").asText()).isEqualTo("gpt-4o-mini");
        assertThat(call.body().path("temperature").asDouble()).isEqualTo(0.3);
        String messages = allMessages(call.body());
        assertThat(messages).contains("system: Jesteś doświadczonym egzaminatorem", "user: # ZADANIE",
                "Streamy", "- Czym jest stream?", "porcja 3 z 6");
    }

    @Test
    void note_port_uses_the_same_http_path() {
        responder = body -> completion("""
                {"title":"Rekurencja","summary":"S","simpleExplanation":"P","sections":[{"heading":"H","content":"C"}],
                 "examples":[{"title":"a","level":"FOR_CHILD","content":"x","explanation":"y"},
                             {"title":"b","level":"CONTRASTIVE","content":"x","explanation":"y"},
                             {"title":"c","level":"ADVANCED","content":"x","explanation":"y"}],
                 "commonMistakes":["m"],"memoryPoints":["p"],"suggestedSearchPhrases":["q"]}
                """);
        NoteGenerationPort port = new AiConfig().noteGenerationPort(config());

        NoteContent note = port.generate("Rekurencja", "krótko", NoteDifficulty.BASIC);
        NoteContent refined = port.refine("Rekurencja", "dodaj przykład", "stara treść");

        assertThat(note.getTitle()).isEqualTo("Rekurencja");
        assertThat(refined.getExamples()).hasSize(3);
        assertThat(calls).hasSize(2);
        assertThat(allMessages(calls.get(0).body())).contains("Jesteś ekspertem dydaktycznym", "krótko");
        assertThat(allMessages(calls.get(1).body())).contains("Jesteś redaktorem", "dodaj przykład", "stara treść");
    }

    @Test
    void rejected_key_and_garbage_reply_become_bad_gateway_errors() {
        responder = body -> new Reply(401, """
                {"error":{"message":"Incorrect API key provided","type":"invalid_request_error","code":"invalid_api_key"}}
                """);
        QuizGenerationPort quiz = new AiConfig().quizGenerationPort(config());
        QuizChunk chunk = new QuizChunk("T", "n", null, QuizDifficulty.EASY, 5, null, List.of(), 0, 1);
        assertThatThrownBy(() -> quiz.generate(chunk))
                .isInstanceOf(AiResponseException.class).hasMessageContaining("AI request failed");

        responder = body -> completion("Przepraszam, nie mogę tego zrobić.");
        assertThatThrownBy(() -> quiz.generate(chunk)).isInstanceOf(AiResponseException.class);

        NoteGenerationPort note = new AiConfig().noteGenerationPort(config());
        assertThatThrownBy(() -> note.generate("T", null, NoteDifficulty.MEDIUM)).isInstanceOf(AiResponseException.class);
    }

    @Test
    void transient_server_errors_are_retried_a_bounded_number_of_times() {
        AtomicInteger n = new AtomicInteger();
        responder = body -> n.incrementAndGet() <= 2
                ? new Reply(500, "{\"error\":{\"message\":\"overloaded\"}}")
                : completion("{\"questions\":[{\"difficulty\":\"EASY\",\"question\":\"q\",\"code\":\"\","
                        + "\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correctIndex\":1,\"explanation\":\"e\"}]}");
        QuizGenerationPort quiz = new AiConfig().quizGenerationPort(config());
        QuizChunk chunk = new QuizChunk("T", "n", null, QuizDifficulty.EASY, 1, null, List.of(), 0, 1);

        assertThat(quiz.generate(chunk).questions()).hasSize(1);
        assertThat(calls).hasSize(3);

        calls.clear();
        responder = body -> new Reply(503, "{\"error\":{\"message\":\"down\"}}");
        long start = System.nanoTime();
        assertThatThrownBy(() -> quiz.generate(chunk)).isInstanceOf(AiResponseException.class);
        assertThat(calls).hasSize(3);
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - start)).isLessThan(java.time.Duration.ofSeconds(15));
    }
}
