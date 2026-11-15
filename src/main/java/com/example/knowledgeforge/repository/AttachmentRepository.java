package com.example.knowledgeforge.repository;

import com.example.knowledgeforge.domain.attachment.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** Dostęp do tabeli attachment (dawne AttachmentDao) — wyłącznie metadane + względna ścieżka pliku. */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAllByTopicIdOrderByCreatedAtDesc(UUID topicId);

    default List<Attachment> findAllByTopicId(UUID topicId) {
        return findAllByTopicIdOrderByCreatedAtDesc(topicId);
    }

    /** Wszystkie relative_path w bazie — używane wyłącznie przez diagnostykę spójności dysk<->baza. */
    @Query("select a.relativePath from Attachment a")
    List<String> findAllRelativePaths();

    /** INSERT — id nadaje baza (BIGSERIAL) i trafia do tej samej instancji. */
    default Attachment insert(Attachment a) {
        if (a.getCreatedAt() == null) a.setCreatedAt(Instant.now());
        Attachment saved = save(a);
        Logger.getLogger(AttachmentRepository.class.getName())
                .fine(() -> "Inserted attachment " + saved.getId() + " for topic " + saved.getTopicId());
        return saved;
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Attachment a where a.id = :id")
    int deleteRowById(@Param("id") Long id);

    default void delete(Long id) {
        deleteRowById(id);
        Logger.getLogger(AttachmentRepository.class.getName()).fine(() -> "Deleted attachment " + id);
    }
}
