package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.quiz.NoOpQuizGenerationService;
import com.example.knowledgeforge.quiz.QuizGenerationPort;
import com.example.knowledgeforge.quiz.SpringAiQuizGenerationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

/**
 * Wybór implementacji AI (notatki i quizy): pusty OPENAI_API_KEY -> stuby (503 z instrukcją),
 * ustawiony -> OpenAI przez Spring AI (model/temperatura/adres z .env).
 */
@Configuration
public class AiConfig {

    private static final Logger log = Logger.getLogger(AiConfig.class.getName());

    @Bean
    public NoteGenerationPort noteGenerationPort(AppConfig config) {
        if (!enabled(config)) {
            log.info("OPENAI_API_KEY not set — AI note generation disabled (stub active)");
            return new NoOpNoteGenerationService();
        }
        log.info(() -> "AI note generation enabled — model " + config.openAiModel());
        return new SpringAiNoteGenerationService(chatClient(config), config.openAiModel());
    }

    @Bean
    public QuizGenerationPort quizGenerationPort(AppConfig config) {
        if (!enabled(config)) {
            log.info("OPENAI_API_KEY not set — AI quiz generation disabled (stub active)");
            return new NoOpQuizGenerationService();
        }
        log.info(() -> "AI quiz generation enabled — model " + config.openAiModel());
        return new SpringAiQuizGenerationService(chatClient(config), config.openAiModel());
    }

    private static boolean enabled(AppConfig config) {
        String apiKey = config.openAiApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    static ChatClient chatClient(AppConfig config) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.openAiBaseUrl())
                .apiKey(config.openAiApiKey())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.openAiModel())
                        .temperature(config.openAiTemperature())
                        .build())
                .build();
        return ChatClient.create(chatModel);
    }
}
