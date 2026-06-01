package com.example.offerbrowserprototype.domain.ai;

import com.example.offerbrowserprototype.domain.ai.port.NoteGenerationPort;
import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Real AI implementation — active when knowledge.ai.openai.enabled=true.
 * Requires OPENAI_API_KEY environment variable to be set.
 */
@Service
@Primary
@ConditionalOnProperty(name = "knowledge.ai.openai.enabled", havingValue = "true")
public class SpringAiNoteGenerationService implements NoteGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiNoteGenerationService.class);

    private final ChatClient chatClient;

    public SpringAiNoteGenerationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public NoteContent generate(String topicTitle, String userInstruction, Difficulty difficulty) {
        log.info("Generating note via Spring AI: topic='{}', difficulty={}", topicTitle, difficulty);

        NoteContent result = chatClient.prompt()
                .system(NotePromptBuilder.systemPrompt())
                .user(NotePromptBuilder.userPrompt(topicTitle, userInstruction, difficulty))
                .call()
                .entity(NoteContent.class);

        validate(result);
        log.info("Note generated successfully for topic='{}'", topicTitle);
        return result;
    }

    private void validate(NoteContent c) {
        if (c == null)                                    throw new AiResponseException("AI returned null response");
        if (c.getTitle() == null || c.getTitle().isBlank()) throw new AiResponseException("AI response missing title");
        if (c.getSections() == null || c.getSections().isEmpty()) throw new AiResponseException("AI response missing sections");
        if (c.getExamples() == null || c.getExamples().size() < 3) throw new AiResponseException("AI response has fewer than 3 examples");
        if (c.getMemoryPoints() == null || c.getMemoryPoints().isEmpty()) throw new AiResponseException("AI response missing memoryPoints");
    }
}
