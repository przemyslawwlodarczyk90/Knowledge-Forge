package com.example.knowledgeforge.repository;

import com.example.knowledgeforge.domain.note.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dostęp do tabeli note (dawne NoteDao). Baza trzyma tylko ścieżkę do pliku treści
 * (content_path) — sama zawartość .kfdoc leży na dysku, zob. storage.NoteFileStorage.
 */
public interface NoteRepository extends JpaRepository<Note, UUID> {

    Optional<Note> findByTopicId(UUID topicId);

    Optional<Note> findByTopicIdAndUserId(UUID topicId, Long userId);

    /** Wszystkie notatki użytkownika — używane przez wyszukiwarkę (przeszukuje treść). */
    List<Note> findAllByUserId(Long userId);

    /** INSERT — te same wartości domyślne, co dawne NoteDao#insert. */
    default Note insert(Note note) {
        if (note.getId() == null) note.setId(UUID.randomUUID());
        // createdAt bywa ustawiany z wyprzedzeniem przez NoteService (żeby dokładnie ten sam
        // znacznik czasu trafił też do metadanych w pliku .kfdoc) — tu tylko domyślamy, gdy brak.
        if (note.getCreatedAt() == null) note.setCreatedAt(Instant.now());
        note.setUpdatedAt(Instant.now());
        if (note.getVersion() == null) note.setVersion(1);
        return save(note);
    }

    /**
     * Optimistic locking: UPDATE warunkowy na WHERE version = ?. Zwraca liczbę zmienionych wierszy
     * (0 = konflikt) — NIE rzuca wyjątku, żeby wywołujący (NoteService, wewnątrz własnej
     * transakcji) sam zdecydował o rollbacku.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Note n set n.contentPath = :contentPath, n.version = :nextVersion, n.updatedAt = :updatedAt
            where n.id = :id and n.version = :expectedVersion
            """)
    int updateWithVersionCheck(@Param("id") UUID noteId, @Param("contentPath") String contentPath,
                               @Param("nextVersion") int nextVersion, @Param("expectedVersion") int expectedVersion,
                               @Param("updatedAt") Instant updatedAt);

    default int updateWithVersionCheck(UUID noteId, String contentPath, int nextVersion, int expectedVersion) {
        return updateWithVersionCheck(noteId, contentPath, nextVersion, expectedVersion, Instant.now());
    }

    /** Usuwa wiersz notatki tematu (sam plik .kfdoc zostaje na dysku — jak wcześniej). */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Note n where n.topicId = :topicId")
    int deleteRowByTopicId(@Param("topicId") UUID topicId);
}
