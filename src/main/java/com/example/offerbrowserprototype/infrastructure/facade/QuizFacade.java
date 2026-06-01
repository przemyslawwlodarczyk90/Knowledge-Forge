package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.ai.AiUsageGuard;
import com.example.offerbrowserprototype.domain.ai.port.QuizGenerationPort;
import com.example.offerbrowserprototype.domain.exception.ConflictException;
import com.example.offerbrowserprototype.domain.exception.NoteNotFoundException;
import com.example.offerbrowserprototype.domain.exception.QuizNotFoundException;
import com.example.offerbrowserprototype.domain.exception.TopicNotFoundException;
import com.example.offerbrowserprototype.domain.note.Note;
import com.example.offerbrowserprototype.domain.note.NoteContentConverter;
import com.example.offerbrowserprototype.domain.progress.UserProgress;
import com.example.offerbrowserprototype.domain.quiz.Quiz;
import com.example.offerbrowserprototype.domain.quiz.QuizAttempt;
import com.example.offerbrowserprototype.domain.quiz.QuizContent;
import com.example.offerbrowserprototype.domain.quiz.dto.QuizDto;
import com.example.offerbrowserprototype.domain.quiz.dto.QuizResultDto;
import com.example.offerbrowserprototype.domain.quiz.dto.SubmitAttemptRequest;
import com.example.offerbrowserprototype.domain.topic.Difficulty;
import com.example.offerbrowserprototype.domain.topic.Topic;
import com.example.offerbrowserprototype.domain.topic.TopicStatus;
import com.example.offerbrowserprototype.infrastructure.repository.NoteRepository;
import com.example.offerbrowserprototype.infrastructure.repository.QuizAttemptRepository;
import com.example.offerbrowserprototype.infrastructure.repository.QuizRepository;
import com.example.offerbrowserprototype.infrastructure.repository.TopicRepository;
import com.example.offerbrowserprototype.infrastructure.repository.UserProgressRepository;
import com.example.offerbrowserprototype.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizFacade {

    private static final int DEFAULT_QUESTION_COUNT = 10;
    private static final String DEFAULT_PROMPT_VERSION = "v1";

    private final QuizRepository quizRepo;
    private final NoteRepository noteRepo;
    private final TopicRepository topicRepo;
    private final QuizAttemptRepository attemptRepo;
    private final UserProgressRepository progressRepo;
    private final CurrentUser currentUser;
    private final QuizGenerationPort quizGenPort;
    private final AiUsageGuard usageGuard;

    @Transactional(readOnly = true)
    public QuizDto getByTopicId(UUID topicId) {
        Long userId = currentUser.id();
        verifyTopicOwnership(topicId, userId);
        Quiz quiz = quizRepo.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new QuizNotFoundException(topicId.toString()));
        return QuizDto.from(quiz);
    }

    public QuizDto generate(UUID topicId) {
        Long userId = currentUser.id();
        Topic topic = verifyTopicOwnership(topicId, userId);

        // Note must exist before generating quiz
        Note note = noteRepo.findByTopicIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NoteNotFoundException(topicId.toString()));

        usageGuard.checkAndIncrementQuiz(userId);

        String noteContentJson = new NoteContentConverter().convertToDatabaseColumn(note.getContent());

        QuizContent generatedContent = quizGenPort.generate(
                topic.getTitle(),
                noteContentJson,
                topic.getDifficulty(),
                DEFAULT_QUESTION_COUNT,
                DEFAULT_PROMPT_VERSION
        );

        int actualQuestionCount = generatedContent.getQuestions() != null
                ? generatedContent.getQuestions().size()
                : DEFAULT_QUESTION_COUNT;

        Quiz quiz = quizRepo.findByTopicId(topicId)
                .map(existing -> {
                    existing.setContent(generatedContent);
                    existing.setQuestionCount(actualQuestionCount);
                    existing.setPromptVersion(DEFAULT_PROMPT_VERSION);
                    existing.setGeneratedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> Quiz.builder()
                        .userId(userId)
                        .topicId(topicId)
                        .difficulty(topic.getDifficulty())
                        .questionCount(actualQuestionCount)
                        .content(generatedContent)
                        .promptVersion(DEFAULT_PROMPT_VERSION)
                        .generatedAt(Instant.now())
                        .build());

        Quiz saved = quizRepo.save(quiz);

        topic.setStatus(TopicStatus.QUIZ_READY);
        topicRepo.save(topic);

        return QuizDto.from(saved);
    }

    public QuizResultDto submitAttempt(SubmitAttemptRequest req) {
        Long userId = currentUser.id();

        Quiz quiz = quizRepo.findById(req.quizId())
                .orElseThrow(() -> new QuizNotFoundException(req.quizId().toString()));

        // Verify user owns the quiz via topic
        Topic topic = verifyTopicOwnership(quiz.getTopicId(), userId);

        List<QuizContent.Question> questions = quiz.getContent().getQuestions();
        List<String> answers = req.answers();

        int total = questions.size();
        int correct = 0;

        for (int i = 0; i < total && i < answers.size(); i++) {
            String userAnswer = answers.get(i);
            String correctAnswer = questions.get(i).getCorrectAnswer();
            if (correctAnswer != null && correctAnswer.equalsIgnoreCase(userAnswer)) {
                correct++;
            }
        }

        int scorePercent = total > 0 ? (int) Math.round((double) correct / total * 100) : 0;
        boolean passed = scorePercent >= 80;

        // Calculate points
        int basePoints = switch (quiz.getDifficulty()) {
            case BASIC -> 50;
            case MEDIUM -> 120;
            case HARD -> 300;
        };

        double multiplier = 1.0;
        if (scorePercent == 100) {
            multiplier *= 1.25;
        }

        // Check if first attempt
        long previousAttempts = attemptRepo.countByUserIdAndQuizId(userId, quiz.getId());
        if (previousAttempts == 0) {
            multiplier *= 1.20;
        }

        int pointsEarned = passed ? (int) Math.round(basePoints * multiplier) : 0;

        // Save attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .userId(userId)
                .quizId(quiz.getId())
                .correctAnswers(correct)
                .totalQuestions(total)
                .scorePercent(scorePercent)
                .passed(passed)
                .startedAt(Instant.now())
                .finishedAt(Instant.now())
                .build();

        QuizAttempt savedAttempt = attemptRepo.save(attempt);

        // Update UserProgress
        UserProgress progress = progressRepo.findByUserIdAndTopicId(userId, topic.getId())
                .orElseGet(() -> UserProgress.builder()
                        .userId(userId)
                        .topicId(topic.getId())
                        .bestScore(0)
                        .passed(false)
                        .pointsEarned(0)
                        .attemptsCount(0)
                        .build());

        progress.setAttemptsCount(progress.getAttemptsCount() + 1);
        progress.setLastAttemptAt(Instant.now());

        if (scorePercent > progress.getBestScore()) {
            progress.setBestScore(scorePercent);
        }

        if (passed) {
            progress.setPassed(true);
            progress.setPointsEarned(progress.getPointsEarned() + pointsEarned);
        }

        progressRepo.save(progress);

        // Update topic status
        if (passed && progress.getAttemptsCount() >= 2 && scorePercent >= 90) {
            topic.setStatus(TopicStatus.MASTERED);
        } else if (passed) {
            topic.setStatus(TopicStatus.PASSED);
        }
        topicRepo.save(topic);

        return new QuizResultDto(
                savedAttempt.getId(),
                correct,
                total,
                scorePercent,
                passed,
                pointsEarned
        );
    }

    private Topic verifyTopicOwnership(UUID topicId, Long userId) {
        return topicRepo.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }
}
