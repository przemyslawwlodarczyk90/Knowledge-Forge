package com.example.offerbrowserprototype.domain.topic.dto;

import com.example.offerbrowserprototype.domain.topic.Difficulty;
import com.example.offerbrowserprototype.domain.topic.Topic;
import com.example.offerbrowserprototype.domain.topic.TopicStatus;

import java.time.Instant;
import java.util.UUID;

public class TopicDto {

    private UUID id;
    private Long userId;
    private UUID categoryId;
    private String title;
    private String shortPrompt;
    private Difficulty difficulty;
    private TopicStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public TopicDto() {
    }

    public TopicDto(UUID id, Long userId, UUID categoryId, String title, String shortPrompt,
                    Difficulty difficulty, TopicStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.shortPrompt = shortPrompt;
        this.difficulty = difficulty;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TopicDto from(Topic t) {
        return new TopicDto(
                t.getId(),
                t.getUserId(),
                t.getCategoryId(),
                t.getTitle(),
                t.getShortPrompt(),
                t.getDifficulty(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortPrompt() {
        return shortPrompt;
    }

    public void setShortPrompt(String shortPrompt) {
        this.shortPrompt = shortPrompt;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public TopicStatus getStatus() {
        return status;
    }

    public void setStatus(TopicStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
