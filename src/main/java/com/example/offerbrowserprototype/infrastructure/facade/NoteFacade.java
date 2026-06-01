package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.ai.AiUsageGuard;
import com.example.offerbrowserprototype.domain.ai.port.NoteGenerationPort;
import com.example.offerbrowserprototype.domain.exception.ConflictException;
import com.example.offerbrowserprototype.domain.exception.NoteNotFoundException;
import com.example.offerbrowserprototype.domain.exception.TopicNotFoundException;
import com.example.offerbrowserprototype.domain.note.Note;
import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.note.dto.NoteDto;
import com.example.offerbrowserprototype.domain.topic.Topic;
import com.example.offerbrowserprototype.domain.topic.TopicStatus;
import com.example.offerbrowserprototype.infrastructure.repository.NoteRepository;
import com.example.offerbrowserprototype.infrastructure.repository.TopicRepository;
import com.example.offerbrowserprototype.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteFacade {

    private static final String DEFAULT_PROMPT_VERSION = "v1";

    private final NoteRepository noteRepo;
    private final TopicRepository topicRepo;
    private final CurrentUser currentUser;
    private final NoteGenerationPort noteGenPort;
    private final AiUsageGuard usageGuard;

    @Transactional(readOnly = true)
    public NoteDto getByTopicId(UUID topicId) {
        Long userId = currentUser.id();
        verifyTopicOwnership(topicId, userId);
        Note note = noteRepo.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NoteNotFoundException(topicId.toString()));
        return NoteDto.from(note);
    }

    public NoteDto createManual(UUID topicId, NoteContent content) {
        Long userId = currentUser.id();
        verifyTopicOwnership(topicId, userId);

        if (noteRepo.existsByTopicId(topicId)) {
            throw new ConflictException("Note already exists for topic: " + topicId + ". Use update instead.");
        }

        Note note = Note.builder()
                .userId(userId)
                .topicId(topicId)
                .content(content)
                .version(1)
                .build();

        return NoteDto.from(noteRepo.save(note));
    }

    public NoteDto updateManual(UUID topicId, NoteContent content) {
        Long userId = currentUser.id();
        verifyTopicOwnership(topicId, userId);

        Note note = noteRepo.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NoteNotFoundException(topicId.toString()));

        note.setContent(content);
        note.setVersion(note.getVersion() + 1);

        return NoteDto.from(noteRepo.save(note));
    }

    public NoteDto generate(UUID topicId) {
        Long userId = currentUser.id();
        Topic topic = verifyTopicOwnership(topicId, userId);

        if (noteRepo.existsByTopicId(topicId)) {
            throw new ConflictException("Note already exists, use /regenerate");
        }

        usageGuard.checkAndIncrementNote(userId);

        NoteContent generatedContent = noteGenPort.generate(
                topic.getTitle(),
                topic.getShortPrompt(),
                topic.getDifficulty(),
                DEFAULT_PROMPT_VERSION
        );

        Note note = Note.builder()
                .userId(userId)
                .topicId(topicId)
                .content(generatedContent)
                .promptVersion(DEFAULT_PROMPT_VERSION)
                .version(1)
                .generatedAt(Instant.now())
                .build();

        Note saved = noteRepo.save(note);

        topic.setStatus(TopicStatus.NOTE_GENERATED);
        topicRepo.save(topic);

        return NoteDto.from(saved);
    }

    public NoteDto regenerate(UUID topicId) {
        Long userId = currentUser.id();
        Topic topic = verifyTopicOwnership(topicId, userId);

        usageGuard.checkAndIncrementNote(userId);

        NoteContent generatedContent = noteGenPort.generate(
                topic.getTitle(),
                topic.getShortPrompt(),
                topic.getDifficulty(),
                DEFAULT_PROMPT_VERSION
        );

        Note note = noteRepo.findByTopicId(topicId)
                .map(existing -> {
                    existing.setContent(generatedContent);
                    existing.setVersion(existing.getVersion() + 1);
                    existing.setPromptVersion(DEFAULT_PROMPT_VERSION);
                    existing.setGeneratedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> Note.builder()
                        .userId(userId)
                        .topicId(topicId)
                        .content(generatedContent)
                        .promptVersion(DEFAULT_PROMPT_VERSION)
                        .version(1)
                        .generatedAt(Instant.now())
                        .build());

        Note saved = noteRepo.save(note);

        topic.setStatus(TopicStatus.NOTE_GENERATED);
        topicRepo.save(topic);

        return NoteDto.from(saved);
    }

    private Topic verifyTopicOwnership(UUID topicId, Long userId) {
        return topicRepo.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }
}
