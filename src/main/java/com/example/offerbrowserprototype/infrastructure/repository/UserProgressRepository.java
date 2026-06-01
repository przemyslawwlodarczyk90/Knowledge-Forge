package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.progress.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    Optional<UserProgress> findByUserIdAndTopicId(Long userId, UUID topicId);

    List<UserProgress> findAllByUserId(Long userId);

    long countByUserIdAndPassedTrue(Long userId);

    long countByUserId(Long userId);
}
