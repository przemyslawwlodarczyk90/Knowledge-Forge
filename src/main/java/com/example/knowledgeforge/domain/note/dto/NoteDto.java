package com.example.knowledgeforge.domain.note.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public class NoteDto {

    private UUID id;
    private UUID topicId;
    /** Drzewo dokumentu edytora, z odnośnikami do obrazków przepisanymi na realne URL-e. */
    private JsonNode contentJson;
    private Integer version;
    private Instant updatedAt;

    public NoteDto() {
    }

    public NoteDto(UUID id, UUID topicId, JsonNode contentJson, Integer version, Instant updatedAt) {
        this.id = id;
        this.topicId = topicId;
        this.contentJson = contentJson;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTopicId() { return topicId; }
    public void setTopicId(UUID topicId) { this.topicId = topicId; }

    public JsonNode getContentJson() { return contentJson; }
    public void setContentJson(JsonNode contentJson) { this.contentJson = contentJson; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
