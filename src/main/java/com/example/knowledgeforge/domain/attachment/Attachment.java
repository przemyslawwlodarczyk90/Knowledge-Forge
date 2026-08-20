package com.example.knowledgeforge.domain.attachment;

import java.time.Instant;
import java.util.UUID;

/**
 * Model domenowy załącznika. Baza trzyma wyłącznie metadane i relativePath —
 * bajty pliku leżą na dysku pod attachments.storage.path (zob. storage.AttachmentStorage).
 * Ani relativePath, ani storedName nigdy nie trafiają do odpowiedzi HTTP (zob. dto.AttachmentDto) —
 * publiczne adresy (/api/attachments/{id}/view|download) są generowane wyłącznie na podstawie id.
 */
public class Attachment {

    private Long id;
    private UUID topicId;
    private String originalName;
    private String storedName;
    private String relativePath;
    private String contentType;
    private long sizeBytes;
    private AttachmentType attachmentType;
    private String description;
    private String checksumSha256;
    private Instant createdAt;

    public Attachment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getTopicId() { return topicId; }
    public void setTopicId(UUID topicId) { this.topicId = topicId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public AttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(AttachmentType attachmentType) { this.attachmentType = attachmentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
