package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.exception.CategoryNotFoundException;
import com.example.offerbrowserprototype.domain.exception.TopicNotFoundException;
import com.example.offerbrowserprototype.domain.topic.Topic;
import com.example.offerbrowserprototype.domain.topic.TopicStatus;
import com.example.offerbrowserprototype.domain.topic.dto.CreateTopicRequest;
import com.example.offerbrowserprototype.domain.topic.dto.TopicDto;
import com.example.offerbrowserprototype.domain.topic.dto.UpdateTopicRequest;
import com.example.offerbrowserprototype.infrastructure.repository.CategoryRepository;
import com.example.offerbrowserprototype.infrastructure.repository.TopicRepository;
import com.example.offerbrowserprototype.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicFacade {

    private final TopicRepository topicRepo;
    private final CategoryRepository categoryRepo;
    private final CurrentUser currentUser;

    public TopicDto create(CreateTopicRequest req) {
        Long userId = currentUser.id();

        // Verify category belongs to this user
        categoryRepo.findByIdAndUserId(req.categoryId(), userId)
                .orElseThrow(() -> new CategoryNotFoundException(req.categoryId().toString()));

        Topic topic = Topic.builder()
                .userId(userId)
                .categoryId(req.categoryId())
                .title(req.title())
                .shortPrompt(req.shortPrompt())
                .difficulty(req.difficulty())
                .status(TopicStatus.NEW)
                .code(req.code())
                .build();

        return TopicDto.from(topicRepo.save(topic));
    }

    @Transactional(readOnly = true)
    public TopicDto getById(UUID id) {
        Long userId = currentUser.id();
        Topic topic = topicRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));
        return TopicDto.from(topic);
    }

    @Transactional(readOnly = true)
    public List<TopicDto> listByCategory(UUID categoryId) {
        Long userId = currentUser.id();

        // Verify category belongs to this user
        categoryRepo.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId.toString()));

        return topicRepo.findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(userId, categoryId)
                .stream()
                .map(TopicDto::from)
                .collect(Collectors.toList());
    }

    public TopicDto update(UUID id, UpdateTopicRequest req) {
        Long userId = currentUser.id();
        Topic topic = topicRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));

        if (req.title() != null && !req.title().isBlank()) {
            topic.setTitle(req.title());
        }
        if (req.shortPrompt() != null) {
            topic.setShortPrompt(req.shortPrompt());
        }
        if (req.difficulty() != null) {
            topic.setDifficulty(req.difficulty());
        }

        return TopicDto.from(topicRepo.save(topic));
    }

    public void delete(UUID id) {
        Long userId = currentUser.id();
        Topic topic = topicRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TopicNotFoundException(id.toString()));
        topicRepo.delete(topic);
    }
}
