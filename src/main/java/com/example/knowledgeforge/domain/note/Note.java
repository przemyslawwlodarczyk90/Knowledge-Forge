package com.example.knowledgeforge.domain.note;

import java.time.Instant;
import java.util.UUID;

public class Note {

    private UUID id;
    private Long userId;
    private UUID topicId;
    /** Ścieżka na dysku do pliku .kfdoc (treść edytora + wklejone obrazki). Baza trzyma tylko tę ścieżkę. */
    private String contentPath;
    private Integer version = 1;
    private Instant createdAt;
    private Instant updatedAt;

    public Note() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public UUID getTopicId() { return topicId; }
    public void setTopicId(UUID topicId) { this.topicId = topicId; }

    public String getContentPath() { return contentPath; }
    public void setContentPath(String contentPath) { this.contentPath = contentPath; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
