package com.example.knowledgeforge.domain.topic.dto;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.TopicType;

import java.time.Instant;
import java.util.UUID;

public class TopicDto {

    private UUID id;
    private Long userId;
    private UUID categoryId;
    private String title;
    private String shortPrompt;
    private String author;
    private DetailLevel detailLevel;
    private TopicType type;
    private TopicStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public TopicDto() {
    }

    public TopicDto(UUID id, Long userId, UUID categoryId, String title, String shortPrompt, String author,
                    DetailLevel detailLevel, TopicType type, TopicStatus status,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.shortPrompt = shortPrompt;
        this.author = author;
        this.detailLevel = detailLevel;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TopicDto from(Topic t) {
        return new TopicDto(
                t.getId(), t.getUserId(), t.getCategoryId(), t.getTitle(), t.getShortPrompt(), t.getAuthor(),
                t.getDetailLevel(), t.getType(), t.getStatus(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getShortPrompt() { return shortPrompt; }
    public void setShortPrompt(String shortPrompt) { this.shortPrompt = shortPrompt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public DetailLevel getDetailLevel() { return detailLevel; }
    public void setDetailLevel(DetailLevel detailLevel) { this.detailLevel = detailLevel; }

    public TopicType getType() { return type; }
    public void setType(TopicType type) { this.type = type; }

    public TopicStatus getStatus() { return status; }
    public void setStatus(TopicStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
