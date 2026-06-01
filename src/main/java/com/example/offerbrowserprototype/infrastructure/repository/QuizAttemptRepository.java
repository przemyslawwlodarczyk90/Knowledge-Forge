package com.example.offerbrowserprototype.infrastructure.repository;

import com.example.offerbrowserprototype.domain.quiz.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findAllByUserIdAndQuizIdOrderByStartedAtDesc(Long userId, UUID quizId);

    long countByUserIdAndQuizId(Long userId, UUID quizId);

    Optional<QuizAttempt> findTopByUserIdAndQuizIdOrderByScorePercentDesc(Long userId, UUID quizId);
}
