package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.domain.exception.AiResponseException;
import org.springframework.ai.chat.client.ChatClient;

import java.util.logging.Logger;

/** OpenAI przez Spring AI ChatClient — odpowiedź mapowana wprost na {@link QuizBatch}. */
public class SpringAiQuizGenerationService implements QuizGenerationPort {

    private static final Logger log = Logger.getLogger(SpringAiQuizGenerationService.class.getName());

    private final ChatClient chatClient;
    private final String model;

    public SpringAiQuizGenerationService(ChatClient chatClient, String model) {
        this.chatClient = chatClient;
        this.model = model;
    }

    @Override
    public QuizBatch generate(QuizChunk chunk) {
        log.info(() -> "Requesting quiz chunk " + (chunk.index() + 1) + "/" + chunk.total()
                + " (" + chunk.difficulty() + " x" + chunk.count() + ") for topic='" + chunk.topicTitle() + "'");
        QuizBatch batch;
        try {
            batch = chatClient.prompt()
                    .system(QuizPrompt.systemPrompt())
                    .user(QuizPrompt.userPrompt(chunk))
                    .call()
                    .entity(QuizBatch.class);
        } catch (RuntimeException e) {
            throw new AiResponseException("AI request failed: " + e.getMessage());
        }
        if (batch == null || batch.questions() == null || batch.questions().isEmpty()) {
            throw new AiResponseException("AI returned no quiz questions");
        }
        return batch;
    }

    @Override
    public String modelName() {
        return model;
    }
}
