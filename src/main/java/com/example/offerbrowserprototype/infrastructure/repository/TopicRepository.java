package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findByIdAndUserId(UUID id, Long userId);

    List<Topic> findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, UUID categoryId);

    boolean existsByUserIdAndCategoryId(Long userId, UUID categoryId);

    long countByUserIdAndCategoryId(Long userId, UUID categoryId);
}
