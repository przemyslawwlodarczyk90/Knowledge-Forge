package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;
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

/** Prawdziwy ChatClient Spring AI na sztucznym ChatModel — bez sieci, ale z prawdziwym parsowaniem JSON. */
class SpringAiNoteGenerationServiceTest {

    static final String VALID_JSON = """
            {"title":"Rekurencja","summary":"S","simpleExplanation":"P",
             "sections":[{"heading":"H","content":"C"}],
             "examples":[{"title":"a","level":"FOR_CHILD","content":"x","explanation":"y"},
                         {"title":"b","level":"CONTRASTIVE","content":"x","explanation":"y"},
                         {"title":"c","level":"ADVANCED","content":"x","explanation":"y"}],
             "commonMistakes":["m"],"memoryPoints":["p"],"suggestedSearchPhrases":["q"]}
            """;

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
    void sends_system_and_user_prompt_and_parses_structured_reply() {
        AtomicReference<Prompt> sent = new AtomicReference<>();
        var service = new SpringAiNoteGenerationService(ChatClient.create(fixed(VALID_JSON, sent)), "gpt-4o-mini");

        NoteContent c = service.generate("Rekurencja", "krótko", NoteDifficulty.MEDIUM);

        assertThat(c.getTitle()).isEqualTo("Rekurencja");
        assertThat(c.getExamples()).hasSize(3);
        assertThat(c.getExamples().get(2).getLevel()).isEqualTo(NoteContent.ExampleLevel.ADVANCED);
        assertThat(service.modelName()).isEqualTo("gpt-4o-mini");
        String all = sent.get().getContents();
        assertThat(all).contains("Jesteś ekspertem dydaktycznym").contains("Rekurencja").contains("krótko");
    }

    @Test
    void incomplete_or_broken_reply_becomes_ai_response_exception() {
        var missingExamples = new SpringAiNoteGenerationService(ChatClient.create(fixed(
                "{\"title\":\"T\",\"sections\":[{\"heading\":\"h\",\"content\":\"c\"}],\"examples\":[],\"memoryPoints\":[\"p\"]}",
                new AtomicReference<>())), "m");
        assertThatThrownBy(() -> missingExamples.generate("T", null, NoteDifficulty.BASIC))
                .isInstanceOf(AiResponseException.class).hasMessageContaining("fewer than 3 examples");

        var notJson = new SpringAiNoteGenerationService(ChatClient.create(fixed("to nie jest json", new AtomicReference<>())), "m");
        assertThatThrownBy(() -> notJson.generate("T", null, NoteDifficulty.BASIC)).isInstanceOf(AiResponseException.class);
    }

    @Test
    void validation_rules() {
        assertThatThrownBy(() -> SpringAiNoteGenerationService.validate(null)).hasMessageContaining("null");
        NoteContent c = new NoteContent();
        assertThatThrownBy(() -> SpringAiNoteGenerationService.validate(c)).hasMessageContaining("title");
        c.setTitle("t");
        assertThatThrownBy(() -> SpringAiNoteGenerationService.validate(c)).hasMessageContaining("sections");
        c.setSections(List.of(new NoteContent.Section()));
        c.setExamples(List.of(new NoteContent.Example(), new NoteContent.Example(), new NoteContent.Example()));
        assertThatThrownBy(() -> SpringAiNoteGenerationService.validate(c)).hasMessageContaining("memoryPoints");
        c.setMemoryPoints(List.of("p"));
        SpringAiNoteGenerationService.validate(c);
    }

    @Test
    void stub_explains_how_to_enable_ai() {
        var stub = new NoOpNoteGenerationService();
        assertThat(stub.modelName()).isEqualTo("none");
        assertThatThrownBy(() -> stub.generate("t", null, NoteDifficulty.BASIC))
                .isInstanceOf(AiNotConfiguredException.class).hasMessageContaining("OPENAI_API_KEY");
    }
}
