package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.ai.dto.GenerateNoteRequest;
import com.example.knowledgeforge.ai.dto.GeneratedNoteDto;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.TopicNotFoundException;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.repository.TopicRepository;
import com.example.knowledgeforge.service.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generuje szkic notatki dla tematu zalogowanego użytkownika: własność tematu -> dzienny limit ->
 * model -> dokument edytora. Niczego nie zapisuje (poza licznikiem użycia) — użytkownik przegląda
 * szkic w edytorze i zapisuje go normalnie.
 */
@Service
public class NoteGenerationService {

    private final TopicRepository topicRepository;
    private final CurrentUser currentUser;
    private final AiUsageGuard usageGuard;
    private final NoteGenerationPort generationPort;
    private final AppConfig config;

    public NoteGenerationService(TopicRepository topicRepository, CurrentUser currentUser, AiUsageGuard usageGuard,
                                 NoteGenerationPort generationPort, AppConfig config) {
        this.topicRepository = topicRepository;
        this.currentUser = currentUser;
        this.usageGuard = usageGuard;
        this.generationPort = generationPort;
        this.config = config;
    }

    public GeneratedNoteDto generate(UUID topicId, GenerateNoteRequest req) {
        Long userId = currentUser.id();
        Topic topic = topicRepository.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));

        String instruction = req != null && req.instruction() != null && !req.instruction().isBlank()
                ? req.instruction() : topic.getShortPrompt();
        NoteDifficulty difficulty = req != null && req.difficulty() != null
                ? req.difficulty() : NoteDifficulty.fromDetailLevel(topic.getDetailLevel());

        int usedToday = usageGuard.checkAndIncrementNote(userId);
        NoteContent content = generationPort.generate(topic.getTitle(), instruction, difficulty);

        return new GeneratedNoteDto(topicId, content.getTitle(), NoteContentDocumentMapper.toDocument(content),
                difficulty, generationPort.modelName(), NotePromptBuilder.PROMPT_VERSION, usedToday,
                config.aiDailyNoteLimit());
    }
}
