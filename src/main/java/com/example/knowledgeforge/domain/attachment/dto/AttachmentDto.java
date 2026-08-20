package com.example.knowledgeforge.domain.attachment.dto;

import com.example.knowledgeforge.domain.attachment.Attachment;

import java.time.Instant;
import java.util.UUID;

/**
 * Odpowiedź HTTP dla załącznika — celowo BEZ storedName/relativePath (nie ujawniamy
 * układu katalogów na dysku). Frontend buduje adresy /view i /download wyłącznie z {@code id}.
 */
public record AttachmentDto(
        Long id,
        UUID topicId,
        String originalName,
        String description,
        String contentType,
        long sizeBytes,
        String attachmentType,
        String checksumSha256,
        Instant createdAt
) {
    public static AttachmentDto from(Attachment a) {
        return new AttachmentDto(
                a.getId(),
                a.getTopicId(),
                a.getOriginalName(),
                a.getDescription(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getAttachmentType().name(),
                a.getChecksumSha256(),
                a.getCreatedAt()
        );
    }
}
