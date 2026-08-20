package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.CategoryDao;
import com.example.knowledgeforge.dao.NoteDao;
import com.example.knowledgeforge.document.DocumentContainer;
import com.example.knowledgeforge.domain.category.CategoryNode;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.NotFoundException;
import com.example.knowledgeforge.domain.exception.NoteNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.note.Note;
import com.example.knowledgeforge.domain.note.dto.NoteDto;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.storage.NoteFileStorage;
import com.example.knowledgeforge.ws.ApplicationEventHub;
import com.fasterxml.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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

    private final DataSource dataSource;
    private final NoteDao noteDao;
    private final TopicService topicService;
    private final com.example.knowledgeforge.dao.TopicDao topicDao;
    private final CategoryDao categoryDao;
    private final CurrentUser currentUser;
    private final NoteFileStorage fileStorage;
    private final ApplicationEventHub eventHub;

    public NoteService(DataSource dataSource, NoteDao noteDao, TopicService topicService,
                        com.example.knowledgeforge.dao.TopicDao topicDao, CategoryDao categoryDao,
                        CurrentUser currentUser, NoteFileStorage fileStorage, ApplicationEventHub eventHub) {
        this.dataSource = dataSource;
        this.noteDao = noteDao;
        this.topicService = topicService;
        this.topicDao = topicDao;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
        this.fileStorage = fileStorage;
        this.eventHub = eventHub;
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

    /**
     * Zapis notatki/instrukcji — jeden upsert, tak jak "Zapisz" w edytorze tekstu.
     * Optimistic locking na note.version + atomowa zmiana statusu tematu (NEW -> NOTE_ADDED)
     * w tej samej transakcji co zapis notatki — obie zmiany zachodzą razem albo wcale.
     */
    public NoteDto save(UUID topicId, SaveNoteRequest req, String clientId) {
        if (req.contentJson() == null) {
            throw new ValidationException("contentJson must not be null");
        }

        Long userId = currentUser.id();
        Topic topicForMetadata = topicService.verifyOwnership(topicId, userId);
        CategoryNode category = categoryDao.findByIdAndUserId(topicForMetadata.getCategoryId(), userId).orElse(null);

        int assetCount = req.assets() == null ? 0 : req.assets().size();
        log.info(() -> "save " + topicId + ": incoming assets=" + assetCount);

        Note existing = noteDao.findByTopicId(topicId).orElse(null);

        // Szybki, wczesny fail przed jakimkolwiek zapisem na dysk — właściwym (rozstrzygającym)
        // strażnikiem jest i tak warunek "WHERE version = ?" w SQL poniżej; to tylko unika
        // niepotrzebnego zapisu pliku, gdy konflikt jest oczywisty od razu.
        if (existing != null) {
            if (req.baseVersion() == null) {
                throw new ValidationException("baseVersion is required when updating an existing note");
            }
            if (!req.baseVersion().equals(existing.getVersion())) {
                throw new ConflictException("Note was modified by someone else in the meantime");
            }
        }

        UUID noteId = existing != null ? existing.getId() : UUID.randomUUID();
        Instant noteCreatedAt = existing != null ? existing.getCreatedAt() : Instant.now();
        int nextVersion = existing != null ? existing.getVersion() + 1 : 1;

        Map<String, Object> metadata = buildMetadata(topicForMetadata, category, noteId, noteCreatedAt, nextVersion);
        byte[] contentBytes = DocumentContainer.of(req.contentJson(), decodeAssets(req.assets()), metadata).toBytes();
        // UWAGA (ograniczenie architektury NoteFileStorage): ścieżka pliku jest deterministyczna
        // per topicId (zawsze "<topicId>.kfdoc", bez wersjonowania w nazwie), więc zapis na dysk
        // NIE jest częścią transakcji SQL poniżej i nie da się go w pełni cofnąć razem z bazą.
        // Skoro jednak SQL-owy warunek "WHERE version = ?" jest sprawdzany PO zapisie, w razie
        // przegranego wyścigu o ułamek sekundy (rzadkie: dwóch userów mija się dokładnie między
        // powyższym wczesnym sprawdzeniem a tym zapisem) plik na dysku może zostać nadpisany
        // treścią przegranego zapisu, mimo że baza zgłosi konflikt i go odrzuci. To węższe ryzyko
        // niż brak optimistic lockingu w ogóle (bez niego CAŁY zapis, nie tylko wąskie okno,
        // zawsze cicho nadpisywał poprzednika) — pełne domknięcie wymagałoby przepisania
        // NoteFileStorage na pliki wersjonowane, co wykracza poza zakres tego zadania.
        String contentPath = fileStorage.writeContent(topicId, contentBytes);
        log.fine(() -> "save " + topicId + ": wrote content -> " + contentPath);

        Note note;
        TopicStatus finalStatus;
        // MULTI-THREADING:
        // Jedna transakcja JDBC obejmuje: (1) warunkowy UPDATE/INSERT notatki z optimistic
        // lockingiem (WHERE version = ?) i (2) warunkową zmianę statusu tematu NEW -> NOTE_ADDED
        // (też przez WHERE version = ?, na świeżo odczytanej w tej transakcji wersji tematu).
        // Obie zmiany muszą zajść razem albo wcale — inaczej moglibyśmy zapisać notatkę, a status
        // tematu zostawić NEW (albo odwrotnie), co jest złym stanem widocznym dla innych żądań.
        // Zakres blokady/transakcji to WYŁĄCZNIE te dwa zapytania SQL — żadnego I/O na plikach ani
        // komunikacji WebSocket wewnątrz. commit/rollback + zamknięcie połączenia są w finally,
        // zanim cokolwiek zostanie rozgłoszone.
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (existing != null) {
                    int affected = noteDao.updateWithVersionCheck(con, existing.getId(), contentPath, nextVersion, req.baseVersion());
                    if (affected == 0) {
                        con.rollback();
                        throw new ConflictException("Note was modified by someone else in the meantime");
                    }
                    existing.setContentPath(contentPath);
                    existing.setVersion(nextVersion);
                    note = existing;
                    log.info(() -> "save " + topicId + ": updated note " + note.getId() + " -> version=" + note.getVersion());
                } else {
                    Note fresh = new Note();
                    fresh.setId(noteId);
                    fresh.setUserId(userId);
                    fresh.setTopicId(topicId);
                    fresh.setContentPath(contentPath);
                    fresh.setVersion(1);
                    fresh.setCreatedAt(noteCreatedAt);
                    noteDao.insert(con, fresh);
                    note = fresh;
                    log.info(() -> "save " + topicId + ": created note " + note.getId());
                }

                Topic freshTopic = topicDao.findByIdAndUserId(con, topicId, userId)
                        .orElseThrow(() -> new NotFoundException("Topic disappeared during note save: " + topicId));
                if (freshTopic.getStatus() == TopicStatus.NEW) {
                    int affected = topicDao.updateStatus(con, topicId, TopicStatus.NOTE_ADDED, freshTopic.getVersion());
                    if (affected == 0) {
                        con.rollback();
                        throw new ConflictException("Topic was modified by someone else in the meantime");
                    }
                    finalStatus = TopicStatus.NOTE_ADDED;
                } else {
                    finalStatus = freshTopic.getStatus();
                }

                con.commit();
            } catch (Exception e) {
                safeRollback(con);
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open transaction for note save", e);
        }

        eventHub.noteSaved(note.getId(), topicId, note.getVersion(), finalStatus, String.valueOf(userId), clientId);

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

    private static void safeRollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignored) {
        }
    }
}
