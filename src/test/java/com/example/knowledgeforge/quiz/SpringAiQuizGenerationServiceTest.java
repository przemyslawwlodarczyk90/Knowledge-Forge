package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.domain.exception.AiResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prawdziwy ChatClient Spring AI na sztucznym ChatModel — bez sieci, z prawdziwym parsowaniem JSON. */
class SpringAiQuizGenerationServiceTest {

    static final String VALID_JSON = """
            {"questions":[
              {"difficulty":"HARD","question":"Co wypisze kod?","code":"System.out.println(1 + 2 + \\"3\\");",
               "options":["33","123","6","Błąd kompilacji"],"correctIndex":0,
               "explanation":"Dodawanie jest lewostronne: 1 + 2 = 3, potem konkatenacja z \\"3\\"."}
            ]}
            """;

    static final QuizChunk CHUNK = new QuizChunk("Operatory w Javie", "Konkatenacja i dodawanie.", "skup się na kodzie",
            QuizDifficulty.HARD, 20, "analiza przykładu", List.of(), 4, 6);

    static ChatModel fixed(String reply, AtomicReference<Prompt> captured) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                captured.set(prompt);
                return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
            }
        };
    }

    @Test
    void sends_quiz_prompts_and_parses_structured_reply_including_code() {
        AtomicReference<Prompt> sent = new AtomicReference<>();
        var service = new SpringAiQuizGenerationService(ChatClient.create(fixed(VALID_JSON, sent)), "gpt-4o-mini");

        QuizBatch batch = service.generate(CHUNK);

        assertThat(batch.questions()).hasSize(1);
        QuizBatch.Item q = batch.questions().get(0);
        assertThat(q.difficulty()).isEqualTo(QuizDifficulty.HARD);
        assertThat(q.code()).isEqualTo("System.out.println(1 + 2 + \"3\");");
        assertThat(q.options()).containsExactly("33", "123", "6", "Błąd kompilacji");
        assertThat(q.correctIndex()).isZero();
        assertThat(service.modelName()).isEqualTo("gpt-4o-mini");

        String all = sent.get().getContents();
        assertThat(all).contains("Jesteś doświadczonym egzaminatorem", "Operatory w Javie", "skup się na kodzie",
                "DOKŁADNIE 20 pytań", "porcja 5 z 6");
        // Spring AI dokleja schemat JSON odpowiedzi — model wie, jakie pola zwrócić.
        assertThat(all).contains("correctIndex", "options");
    }

    @Test
    void empty_or_broken_reply_becomes_ai_response_exception() {
        var empty = new SpringAiQuizGenerationService(ChatClient.create(fixed("{\"questions\":[]}", new AtomicReference<>())), "m");
        assertThatThrownBy(() -> empty.generate(CHUNK)).isInstanceOf(AiResponseException.class).hasMessageContaining("no quiz");

        var missing = new SpringAiQuizGenerationService(ChatClient.create(fixed("{}", new AtomicReference<>())), "m");
        assertThatThrownBy(() -> missing.generate(CHUNK)).isInstanceOf(AiResponseException.class);

        var notJson = new SpringAiQuizGenerationService(ChatClient.create(fixed("Oto pytania: ...", new AtomicReference<>())), "m");
        assertThatThrownBy(() -> notJson.generate(CHUNK)).isInstanceOf(AiResponseException.class).hasMessageContaining("AI request failed");
    }
}
