package com.example.knowledgeforge.service;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.dao.AttachmentDao;
import com.example.knowledgeforge.dao.TopicDao;
import com.example.knowledgeforge.domain.attachment.Attachment;
import com.example.knowledgeforge.domain.attachment.AttachmentType;
import com.example.knowledgeforge.domain.attachment.dto.AttachmentDto;
import com.example.knowledgeforge.domain.exception.AttachmentNotFoundException;
import com.example.knowledgeforge.domain.exception.TopicNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.storage.AttachmentStorage;
import com.example.knowledgeforge.ws.ApplicationEventHub;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Warstwa biznesowa dla załączników — sklejenie AttachmentDao (metadane w Postgresie) i
 * AttachmentStorage (bajty na dysku). Nigdy nie loguje treści pliku, tylko rozmiary/nazwy/id.
 */
public class AttachmentService {

    private static final Logger log = Logger.getLogger(AttachmentService.class.getName());

    private final AttachmentDao attachmentDao;
    private final AttachmentStorage storage;
    private final TopicDao topicDao;
    private final CurrentUser currentUser;
    private final long maxFileSizeBytes;
    private final AttachmentLockRegistry lockRegistry;
    private final ApplicationEventHub eventHub;

    public AttachmentService(AttachmentDao attachmentDao, AttachmentStorage storage, TopicDao topicDao,
                              CurrentUser currentUser, AppConfig config, AttachmentLockRegistry lockRegistry,
                              ApplicationEventHub eventHub) {
        this.attachmentDao = attachmentDao;
        this.storage = storage;
        this.topicDao = topicDao;
        this.currentUser = currentUser;
        this.maxFileSizeBytes = config.attachmentsMaxFileSizeMb() * 1024L * 1024L;
        this.lockRegistry = lockRegistry;
        this.eventHub = eventHub;
    }

    /** Wynik streamowania do klienta — plik na dysku + metadane potrzebne do nagłówków HTTP. */
    public record ResolvedFile(Attachment attachment, Path path) {
    }

    public List<AttachmentDto> listByTopic(UUID topicId) {
        verifyTopicOwnership(topicId);
        return attachmentDao.findAllByTopicId(topicId).stream().map(AttachmentDto::from).toList();
    }

    /**
     * Zapisuje upload strumieniowo na dysk, klasyfikuje VIDEO/FILE i dopiero potem tworzy rekord
     * w bazie. Jeśli zapis rekordu się nie powiedzie, plik zapisany chwilę wcześniej jest usuwany —
     * żadnego osieroconego pliku bez wpisu w bazie.
     */
    public AttachmentDto upload(UUID topicId, String originalName, String declaredContentType,
                                 InputStream fileStream, String description, String clientId) {
        verifyTopicOwnership(topicId);

        if (originalName == null || originalName.isBlank()) {
            throw new ValidationException("File name must not be blank");
        }

        AttachmentStorage.SaveResult saved;
        try {
            saved = storage.save(fileStream, originalName, maxFileSizeBytes);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to save attachment file for topic " + topicId, e);
            throw new RuntimeException("Failed to save attachment file: " + e.getMessage(), e);
        }

        String extension = extensionOf(saved.storedName());
        AttachmentType type = AttachmentClassifier.classify(
                extension, declaredContentType, saved.probedContentType(), saved.header(), saved.headerLength());

        Attachment a = new Attachment();
        a.setTopicId(topicId);
        a.setOriginalName(sanitizeDisplayName(originalName));
        a.setStoredName(saved.storedName());
        a.setRelativePath(saved.relativePath());
        a.setContentType(resolveContentType(saved.probedContentType(), declaredContentType));
        a.setSizeBytes(saved.sizeBytes());
        a.setAttachmentType(type);
        a.setDescription(blankToNull(description));
        a.setChecksumSha256(saved.checksumSha256());

        try {
            attachmentDao.insert(a);
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "DB insert failed after saving attachment file — deleting orphaned file "
                    + saved.relativePath() + " for topic " + topicId, e);
            storage.delete(saved.relativePath());
            throw e;
        }

        log.info(() -> "Uploaded attachment " + a.getId() + " for topic " + topicId
                + " (" + type + ", " + saved.sizeBytes() + " bytes, name='" + a.getOriginalName() + "')");
        AttachmentDto dto = AttachmentDto.from(a);
        eventHub.attachmentCreated(dto, String.valueOf(currentUser.id()), clientId);
        return dto;
    }

    public AttachmentDto getById(Long id) {
        return AttachmentDto.from(getOwned(id));
    }

    public ResolvedFile resolveForStreaming(Long id) {
        Attachment a = getOwned(id);
        Path path = storage.resolveExisting(a.getRelativePath());
        return new ResolvedFile(a, path);
    }

    public void delete(Long id, String clientId) {
        // MULTI-THREADING: zob. komentarz przy AttachmentLockRegistry — krótka sekcja krytyczna,
        // bez sieciowego I/O w środku (publikacja zdarzenia jest PO zwolnieniu locka).
        UUID topicId = lockRegistry.withLock(id, () -> {
            Attachment a = getOwned(id);
            boolean fileDeleted = storage.delete(a.getRelativePath());
            if (!fileDeleted) {
                log.warning(() -> "Attachment file missing or failed to delete for id=" + id
                        + " (topic " + a.getTopicId() + ") — deleting DB record anyway");
            }
            attachmentDao.delete(id);
            log.info(() -> "Deleted attachment " + id + " (topic " + a.getTopicId() + ")");
            return a.getTopicId();
        });
        eventHub.attachmentDeleted(id, topicId, String.valueOf(currentUser.id()), clientId);
    }

    /**
     * Woła TopicService.delete() PRZED usunięciem rekordu tematu — usuwa pliki z dysku w
     * kontrolowany sposób (z logowaniem niepowodzeń). Same rekordy attachment kasują się
     * automatycznie przez ON DELETE CASCADE, gdy temat zostanie usunięty chwilę później.
     */
    public void deleteFilesForTopic(UUID topicId) {
        List<Attachment> attachments = attachmentDao.findAllByTopicId(topicId);
        for (Attachment a : attachments) {
            boolean ok = storage.delete(a.getRelativePath());
            if (!ok) {
                log.severe(() -> "Failed to delete attachment file during topic cleanup: attachmentId="
                        + a.getId() + " topicId=" + topicId + " relativePath=" + a.getRelativePath());
            }
        }
    }

    private Attachment getOwned(Long id) {
        Attachment a = attachmentDao.findById(id)
                .orElseThrow(() -> new AttachmentNotFoundException(String.valueOf(id)));
        // Załącznik "należy" do tego samego użytkownika co jego temat — jeśli temat nie jest
        // (już) własnością currentUser, traktujemy załącznik jak nieistniejący.
        topicDao.findByIdAndUserId(a.getTopicId(), currentUser.id())
                .orElseThrow(() -> new AttachmentNotFoundException(String.valueOf(id)));
        return a;
    }

    private void verifyTopicOwnership(UUID topicId) {
        topicDao.findByIdAndUserId(topicId, currentUser.id())
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }

    private static String extensionOf(String storedName) {
        int dot = storedName.lastIndexOf('.');
        return dot >= 0 ? storedName.substring(dot + 1) : "";
    }

    private static String resolveContentType(String probed, String declared) {
        if (probed != null && !probed.isBlank()) return probed;
        if (declared != null && !declared.isBlank()) return declared;
        return "application/octet-stream";
    }

    private static String sanitizeDisplayName(String name) {
        String cleaned = name.replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isEmpty()) cleaned = "attachment";
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
