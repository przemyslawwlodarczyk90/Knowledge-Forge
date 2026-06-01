package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.progress.UserProgress;
import com.example.offerbrowserprototype.domain.progress.dto.ProgressSummaryDto;
import com.example.offerbrowserprototype.infrastructure.repository.UserProgressRepository;
import com.example.offerbrowserprototype.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressFacade {

    private final UserProgressRepository progressRepo;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public ProgressSummaryDto getSummary() {
        Long userId = currentUser.id();
        List<UserProgress> allProgress = progressRepo.findAllByUserId(userId);

        long totalTopics = allProgress.size();
        long passedTopics = allProgress.stream().filter(UserProgress::isPassed).count();
        long totalPoints = allProgress.stream()
                .mapToLong(UserProgress::getPointsEarned)
                .sum();
        double averageScore = allProgress.stream()
                .mapToInt(UserProgress::getBestScore)
                .average()
                .orElse(0.0);

        return new ProgressSummaryDto(totalTopics, passedTopics, totalPoints, averageScore);
    }
}
