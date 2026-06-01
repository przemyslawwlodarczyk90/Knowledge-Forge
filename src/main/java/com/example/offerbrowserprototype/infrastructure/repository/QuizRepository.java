package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.quiz.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByTopicId(UUID topicId);

    Optional<Quiz> findByTopicIdAndUserId(UUID topicId, Long userId);

    boolean existsByTopicId(UUID topicId);
}
