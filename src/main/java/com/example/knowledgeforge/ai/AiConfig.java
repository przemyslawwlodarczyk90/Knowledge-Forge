package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

/**
 * Wybór implementacji generowania notatek: pusty OPENAI_API_KEY -> stub (503 z instrukcją),
 * ustawiony -> OpenAI przez Spring AI (model/temperatura z .env).
 */
@Configuration
public class AiConfig {

    private static final Logger log = Logger.getLogger(AiConfig.class.getName());

    @Bean
    public NoteGenerationPort noteGenerationPort(AppConfig config) {
        String apiKey = config.openAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("OPENAI_API_KEY not set — AI note generation disabled (stub active)");
            return new NoOpNoteGenerationService();
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.openAiBaseUrl())
                .apiKey(apiKey)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.openAiModel())
                        .temperature(config.openAiTemperature())
                        .build())
                .build();
        log.info(() -> "AI note generation enabled — model " + config.openAiModel());
        return new SpringAiNoteGenerationService(ChatClient.create(chatModel), config.openAiModel());
    }
}
