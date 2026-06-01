package com.example.offerbrowserprototype.domain.note.dto;

import com.example.offerbrowserprototype.domain.note.Note;
import com.example.offerbrowserprototype.domain.note.NoteContent;

import java.time.Instant;
import java.util.UUID;

public class NoteDto {

    private UUID id;
    private UUID topicId;
    private NoteContent content;
    private String promptVersion;
    private String aiModel;
    private Integer version;
    private Instant generatedAt;

    public NoteDto() {
    }

    public NoteDto(UUID id, UUID topicId, NoteContent content, String promptVersion,
                   String aiModel, Integer version, Instant generatedAt) {
        this.id = id;
        this.topicId = topicId;
        this.content = content;
        this.promptVersion = promptVersion;
        this.aiModel = aiModel;
        this.version = version;
        this.generatedAt = generatedAt;
    }

    public static NoteDto from(Note n) {
        return new NoteDto(
                n.getId(),
                n.getTopicId(),
                n.getContent(),
                n.getPromptVersion(),
                n.getAiModel(),
                n.getVersion(),
                n.getGeneratedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public NoteContent getContent() {
        return content;
    }

    public void setContent(NoteContent content) {
        this.content = content;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
