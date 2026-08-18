package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.document.DocumentContainer;
import com.example.knowledgeforge.domain.category.CategoryNode;
import com.example.knowledgeforge.domain.exception.NotFoundException;
import com.example.knowledgeforge.domain.exception.NoteNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.note.Note;
import com.example.knowledgeforge.domain.note.dto.NoteDto;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Baza danych trzyma tylko ścieżkę do pliku treści (Note.contentPath) —
 * same bajty .kfdoc czyta i pisze {@link NoteFileStorage} na dysku. PDF nie
 * jest tu w ogóle: generuje się wyłącznie w przeglądarce, na żądanie
 * (kliknięcie "Pobierz PDF"), żeby setki notatek nie puchły plikami PDF,
 * których nikt nigdy nie pobierze.
 */
public class NoteService {

    private static final Logger log = Logger.getLogger(NoteService.class.getName());

    private final NoteDao noteDao;
    private final TopicService topicService;
    private final CategoryDao categoryDao;
    private final CurrentUser currentUser;
    private final NoteFileStorage fileStorage;

    public NoteService(NoteDao noteDao, TopicService topicService, CategoryDao categoryDao,
                        CurrentUser currentUser, NoteFileStorage fileStorage) {
        this.noteDao = noteDao;
        this.topicService = topicService;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
        this.fileStorage = fileStorage;
    }

    public NoteDto getByTopicId(UUID topicId) {
        Long userId = currentUser.id();
        topicService.verifyOwnership(topicId, userId);
        Note note = noteDao.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NoteNotFoundException(topicId.toString()));
        log.fine(() -> "getByTopicId " + topicId + " -> version=" + note.getVersion()
                + " contentPath=" + note.getContentPath());
        return toDto(note);
    }

    /** Zapis notatki/instrukcji — jeden upsert, tak jak "Zapisz" w edytorze tekstu. */
    public NoteDto save(UUID topicId, SaveNoteRequest req) {
        if (req.contentJson() == null) {
            throw new ValidationException("contentJson must not be null");
        }

        Long userId = currentUser.id();
        Topic topic = topicService.verifyOwnership(topicId, userId);
        CategoryNode category = categoryDao.findByIdAndUserId(topic.getCategoryId(), userId).orElse(null);

        int assetCount = req.assets() == null ? 0 : req.assets().size();
        log.info(() -> "save " + topicId + ": incoming assets=" + assetCount);

        Note existing = noteDao.findByTopicId(topicId).orElse(null);
        UUID noteId = existing != null ? existing.getId() : UUID.randomUUID();
        Instant noteCreatedAt = existing != null ? existing.getCreatedAt() : Instant.now();
        int nextVersion = existing != null ? existing.getVersion() + 1 : 1;

        Map<String, Object> metadata = buildMetadata(topic, category, noteId, noteCreatedAt, nextVersion);
        byte[] contentBytes = DocumentContainer.of(req.contentJson(), decodeAssets(req.assets()), metadata).toBytes();
        String contentPath = fileStorage.writeContent(topicId, contentBytes);
        log.fine(() -> "save " + topicId + ": wrote content -> " + contentPath);

        Note note;
        if (existing != null) {
            existing.setContentPath(contentPath);
            existing.setVersion(nextVersion);
            noteDao.update(existing);
            note = existing;
            log.info(() -> "save " + topicId + ": updated note " + note.getId() + " -> version=" + note.getVersion());
        } else {
            note = new Note();
            note.setId(noteId);
            note.setUserId(userId);
            note.setTopicId(topicId);
            note.setContentPath(contentPath);
            note.setVersion(1);
            note.setCreatedAt(noteCreatedAt);
            noteDao.insert(note);
            log.info(() -> "save " + topicId + ": created note " + note.getId());
        }

        if (topic.getStatus() == TopicStatus.NEW) {
            topic.setStatus(TopicStatus.NOTE_ADDED);
            topicService.save(topic);
        }

        return toDto(note);
    }

    public byte[] getAsset(UUID topicId, String filename) {
        Long userId = currentUser.id();
        topicService.verifyOwnership(topicId, userId);
        Note note = noteDao.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NoteNotFoundException(topicId.toString()));
        byte[] data = DocumentContainer.fromBytes(fileStorage.read(note.getContentPath())).asset(filename);
        if (data == null) {
            log.warning(() -> "getAsset " + topicId + "/" + filename + ": not found in container");
            throw new NotFoundException("Asset not found: " + filename);
        }
        log.fine(() -> "getAsset " + topicId + "/" + filename + ": " + data.length + " bytes");
        return data;
    }

    private NoteDto toDto(Note note) {
        DocumentContainer container = DocumentContainer.fromBytes(fileStorage.read(note.getContentPath()));
        JsonNode content = container.contentJson();
        DocumentContainer.rewriteImageSrc(content, filename -> "/api/topics/" + note.getTopicId() + "/note/assets/" + filename);
        return new NoteDto(note.getId(), note.getTopicId(), content, note.getVersion(), note.getCreatedAt(), note.getUpdatedAt());
    }

    /**
     * Jawne metadane z encji Topic/Note, dopisywane do manifestu .kfdoc obok samej treści —
     * plik na dysku ma więc być kompletną, samodzielną kopią (metadane + treść), czytelną
     * poza aplikacją zwykłym archiwizatorem ZIP.
     */
    private Map<String, Object> buildMetadata(Topic topic, CategoryNode category, UUID noteId,
                                               Instant noteCreatedAt, int version) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("topicId", topic.getId());
        metadata.put("noteId", noteId);
        metadata.put("title", topic.getTitle());
        metadata.put("shortPrompt", topic.getShortPrompt());
        metadata.put("author", topic.getAuthor());
        metadata.put("type", topic.getType());
        metadata.put("detailLevel", topic.getDetailLevel());
        metadata.put("categoryId", topic.getCategoryId());
        metadata.put("categoryName", category != null ? category.getName() : null);
        metadata.put("topicCreatedAt", topic.getCreatedAt());
        metadata.put("topicUpdatedAt", topic.getUpdatedAt());
        metadata.put("noteCreatedAt", noteCreatedAt);
        metadata.put("noteVersion", version);
        metadata.put("savedAt", Instant.now());
        return metadata;
    }

    private Map<String, byte[]> decodeAssets(List<SaveNoteRequest.AssetPayload> assets) {
        if (assets == null || assets.isEmpty()) return Map.of();
        Map<String, byte[]> map = new HashMap<>();
        for (SaveNoteRequest.AssetPayload asset : assets) {
            map.put(asset.id(), Base64.getDecoder().decode(asset.dataBase64()));
        }
        return map;
    }
}
