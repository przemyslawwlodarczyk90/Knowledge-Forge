package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.document.DocumentContainer;
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
    private final CurrentUser currentUser;
    private final NoteFileStorage fileStorage;

    public NoteService(NoteDao noteDao, TopicService topicService, CurrentUser currentUser, NoteFileStorage fileStorage) {
        this.noteDao = noteDao;
        this.topicService = topicService;
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

        int assetCount = req.assets() == null ? 0 : req.assets().size();
        log.info(() -> "save " + topicId + ": incoming assets=" + assetCount);

        byte[] contentBytes = DocumentContainer.of(req.contentJson(), decodeAssets(req.assets())).toBytes();
        String contentPath = fileStorage.writeContent(topicId, contentBytes);
        log.fine(() -> "save " + topicId + ": wrote content -> " + contentPath);

        Note note = noteDao.findByTopicId(topicId).orElse(null);
        if (note != null) {
            note.setContentPath(contentPath);
            note.setVersion(note.getVersion() + 1);
            noteDao.update(note);
            Note updated = note;
            log.info(() -> "save " + topicId + ": updated note " + updated.getId() + " -> version=" + updated.getVersion());
        } else {
            note = new Note();
            note.setUserId(userId);
            note.setTopicId(topicId);
            note.setContentPath(contentPath);
            note.setVersion(1);
            noteDao.insert(note);
            Note created = note;
            log.info(() -> "save " + topicId + ": created note " + created.getId());
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
        return new NoteDto(note.getId(), note.getTopicId(), content, note.getVersion(), note.getUpdatedAt());
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
