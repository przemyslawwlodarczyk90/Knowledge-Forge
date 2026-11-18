package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.domain.exception.AiResponseException;
import org.springframework.ai.chat.client.ChatClient;

import java.util.logging.Logger;

/** Prawdziwa implementacja — OpenAI przez Spring AI ChatClient; odpowiedź mapowana wprost na NoteContent. */
public class SpringAiNoteGenerationService implements NoteGenerationPort {

    private static final Logger log = Logger.getLogger(SpringAiNoteGenerationService.class.getName());

    private final ChatClient chatClient;
    private final String model;

    public SpringAiNoteGenerationService(ChatClient chatClient, String model) {
        this.chatClient = chatClient;
        this.model = model;
    }

    @Override
    public NoteContent generate(String topicTitle, String userInstruction, NoteDifficulty difficulty) {
        return request(NotePromptBuilder.systemPrompt(),
                NotePromptBuilder.userPrompt(topicTitle, userInstruction, difficulty), topicTitle);
    }

    @Override
    public NoteContent refine(String topicTitle, String userInstruction, String currentNote) {
        return request(NoteRefinementPrompt.SYSTEM_PROMPT,
                NoteRefinementPrompt.userPrompt(topicTitle, userInstruction, currentNote), topicTitle);
    }

    private NoteContent request(String system, String prompt, String topicTitle) {
        log.info(() -> "Requesting AI note draft: topic='" + topicTitle + "'");
        NoteContent result;
        try {
            result = chatClient.prompt()
                    .system(system)
                    .user(prompt)
                    .call()
                    .entity(NoteContent.class);
        } catch (AiResponseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiResponseException("AI request failed: " + e.getMessage());
        }
        validate(result);
        log.info(() -> "Note generated successfully for topic='" + topicTitle + "'");
        return result;
    }

    @Override
    public String modelName() {
        return model;
    }

    static void validate(NoteContent c) {
        if (c == null) throw new AiResponseException("AI returned null response");
        if (c.getTitle() == null || c.getTitle().isBlank()) throw new AiResponseException("AI response missing title");
        if (c.getSections() == null || c.getSections().isEmpty()) throw new AiResponseException("AI response missing sections");
        if (c.getExamples() == null || c.getExamples().size() < 3) throw new AiResponseException("AI response has fewer than 3 examples");
        if (c.getMemoryPoints() == null || c.getMemoryPoints().isEmpty()) throw new AiResponseException("AI response missing memoryPoints");
    }
}
