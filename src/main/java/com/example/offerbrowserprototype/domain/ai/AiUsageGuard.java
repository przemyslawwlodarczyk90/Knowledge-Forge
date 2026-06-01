package com.example.offerbrowserprototype.domain.ai;

import com.example.offerbrowserprototype.domain.exception.DailyLimitExceededException;
import com.example.offerbrowserprototype.infrastructure.repository.AiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AiUsageGuard {

    private static final int DAILY_NOTE_LIMIT = 20;
    private static final int DAILY_QUIZ_LIMIT = 20;

    private final AiUsageRepository repo;

    public void checkAndIncrementNote(Long userId) {
        LocalDate today = LocalDate.now();
        AiUsage usage = repo.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> {
                    AiUsage u = new AiUsage();
                    u.setId(UUID.randomUUID());
                    u.setUserId(userId);
                    u.setUsageDate(today);
                    u.setNoteGenerations(0);
                    u.setQuizGenerations(0);
                    return repo.save(u);
                });
        if (usage.getNoteGenerations() >= DAILY_NOTE_LIMIT) {
            throw new DailyLimitExceededException(
                    "Daily note generation limit reached (" + DAILY_NOTE_LIMIT + ")");
        }
        usage.setNoteGenerations(usage.getNoteGenerations() + 1);
        repo.save(usage);
    }

    public void checkAndIncrementQuiz(Long userId) {
        LocalDate today = LocalDate.now();
        AiUsage usage = repo.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> {
                    AiUsage u = new AiUsage();
                    u.setId(UUID.randomUUID());
                    u.setUserId(userId);
                    u.setUsageDate(today);
                    u.setNoteGenerations(0);
                    u.setQuizGenerations(0);
                    return repo.save(u);
                });
        if (usage.getQuizGenerations() >= DAILY_QUIZ_LIMIT) {
            throw new DailyLimitExceededException(
                    "Daily quiz generation limit reached (" + DAILY_QUIZ_LIMIT + ")");
        }
        usage.setQuizGenerations(usage.getQuizGenerations() + 1);
        repo.save(usage);
    }
}
