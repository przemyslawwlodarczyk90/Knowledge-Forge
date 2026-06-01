package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.note.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    Optional<Note> findByTopicId(UUID topicId);

    Optional<Note> findByTopicIdAndUserId(UUID topicId, Long userId);

    boolean existsByTopicId(UUID topicId);
}
