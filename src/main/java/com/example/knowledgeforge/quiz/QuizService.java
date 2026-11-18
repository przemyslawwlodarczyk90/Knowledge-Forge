package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.ai.AiUsageGuard;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.document.DocumentContainer;
import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;
import com.example.knowledgeforge.domain.exception.AiResponseException;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.TopicNotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.note.Note;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.quiz.dto.GenerateQuizRequest;
import com.example.knowledgeforge.quiz.dto.QuizDto;
import com.example.knowledgeforge.quiz.dto.QuizGenerationResultDto;
import com.example.knowledgeforge.quiz.dto.QuizQuestionDto;
import com.example.knowledgeforge.quiz.dto.QuizStatsDto;
import com.example.knowledgeforge.repository.NoteRepository;
import com.example.knowledgeforge.repository.TopicRepository;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.storage.NoteFileStorage;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quiz z AI dla notatki: pula pytań (rośnie o 100 przy każdym "Wygeneruj quiz z AI") i losowanie
 * świeżego zestawu z całej puli przy każdym podejściu. Kolejność przy generowaniu: własność tematu ->
 * zapisana notatka -> limit puli -> dzienny limit -> równoległe porcje modelu -> walidacja i
 * deduplikacja -> zapis. Porcje, które się nie udały, nie psują pozostałych (wynik mówi, ile ich było).
 */
@Service
public class QuizService {

    private static final Logger log = Logger.getLogger(QuizService.class.getName());

    public static final double PASS_RATIO = 0.75;
    public static final int DEFAULT_QUIZ_LENGTH = 20;
    public static final int MAX_QUIZ_LENGTH = 50;

    private final TopicRepository topicRepository;
    private final NoteRepository noteRepository;
    private final NoteFileStorage noteFileStorage;
    private final QuizQuestionRepository questions;
    private final CurrentUser currentUser;
    private final AiUsageGuard usageGuard;
    private final QuizGenerationPort port;
    private final AppConfig config;
    private final Clock clock;
    private final QuizDraftValidator validator;
    private final ExecutorService executor;

    @Autowired
    public QuizService(TopicRepository topicRepository, NoteRepository noteRepository, NoteFileStorage noteFileStorage,
                       QuizQuestionRepository questions, CurrentUser currentUser, AiUsageGuard usageGuard,
                       QuizGenerationPort port, AppConfig config, Clock clock) {
        this(topicRepository, noteRepository, noteFileStorage, questions, currentUser, usageGuard, port, config, clock,
                new QuizDraftValidator(new SecureRandom()), Executors.newFixedThreadPool(6, daemonThreads()));
    }

    QuizService(TopicRepository topicRepository, NoteRepository noteRepository, NoteFileStorage noteFileStorage,
                QuizQuestionRepository questions, CurrentUser currentUser, AiUsageGuard usageGuard,
                QuizGenerationPort port, AppConfig config, Clock clock, QuizDraftValidator validator,
                ExecutorService executor) {
        this.topicRepository = topicRepository;
        this.noteRepository = noteRepository;
        this.noteFileStorage = noteFileStorage;
        this.questions = questions;
        this.currentUser = currentUser;
        this.usageGuard = usageGuard;
        this.port = port;
        this.config = config;
        this.clock = clock;
        this.validator = validator;
        this.executor = executor;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public QuizStatsDto stats(UUID topicId) {
        Long userId = currentUser.id();
        ownedTopic(topicId, userId);
        return stats(topicId, userId);
    }

    public QuizDto draw(UUID topicId, Integer requested) {
        Long userId = currentUser.id();
        ownedTopic(topicId, userId);
        int count = requested == null ? DEFAULT_QUIZ_LENGTH : requested;
        if (count < 1 || count > MAX_QUIZ_LENGTH) {
            throw new ValidationException("count must be between 1 and " + MAX_QUIZ_LENGTH);
        }
        List<QuizQuestionDto> drawn = questions.drawRandom(topicId, userId, count).stream()
                .map(QuizQuestionDto::of).toList();
        return new QuizDto(drawn, stats(topicId, userId).total(), PASS_RATIO);
    }

    public QuizStatsDto clear(UUID topicId) {
        Long userId = currentUser.id();
        ownedTopic(topicId, userId);
        int removed = questions.deleteByTopic(topicId, userId);
        log.info(() -> "Cleared quiz pool of topic " + topicId + " (" + removed + " questions)");
        return stats(topicId, userId);
    }

    public QuizGenerationResultDto generate(UUID topicId, GenerateQuizRequest req) {
        Long userId = currentUser.id();
        Topic topic = ownedTopic(topicId, userId);
        String noteText = noteText(topicId, userId);

        List<QuizQuestion> existing = questions.findByTopic(topicId, userId);
        int maxPool = config.quizMaxPool();
        if (existing.size() >= maxPool) {
            throw new ConflictException("Pula pytań osiągnęła limit " + maxPool + " — wyczyść ją, aby wygenerować nowe.");
        }

        int usedToday = usageGuard.checkAndIncrementQuiz(userId);
        String instruction = req == null ? null : req.instruction();
        List<QuizPlan.Part> plan = QuizPlan.split(QuizPlan.QUESTIONS_PER_GENERATION);

        List<CompletableFuture<QuizBatch>> futures = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            QuizPlan.Part part = plan.get(i);
            QuizChunk chunk = new QuizChunk(topic.getTitle(), noteText, instruction, part.difficulty(), part.count(),
                    part.angle(), avoidStems(existing, part.difficulty()), i, plan.size());
            futures.add(CompletableFuture.supplyAsync(() -> port.generate(chunk), executor));
        }

        Instant now = clock.instant();
        Set<String> keys = QuizDraftValidator.keys(existing);
        List<QuizQuestion> accepted = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(config.aiQuizTimeoutSeconds());
        for (int i = 0; i < futures.size(); i++) {
            try {
                QuizBatch batch = futures.get(i).get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                accepted.addAll(validator.accept(batch, plan.get(i).difficulty(), topicId, keys, now));
            } catch (TimeoutException e) {
                futures.get(i).cancel(true);
                failures.add(new AiResponseException("AI quiz chunk timed out"));
            } catch (ExecutionException e) {
                failures.add(e.getCause() instanceof RuntimeException re ? re : new AiResponseException(e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiResponseException("Quiz generation interrupted");
            }
        }
        failures.forEach(f -> log.log(Level.WARNING, "Quiz chunk failed for topic " + topicId + ": " + f.getMessage()));

        if (accepted.isEmpty()) {
            throw failures.stream()
                    .filter(f -> f instanceof AiNotConfiguredException).findFirst()
                    .orElseGet(() -> new AiResponseException("AI nie zwróciło żadnego poprawnego pytania — spróbuj ponownie."));
        }
        if (existing.size() + accepted.size() > maxPool) {
            accepted = accepted.subList(0, maxPool - existing.size());
        }
        questions.insertAll(userId, accepted);
        int added = accepted.size();
        log.info(() -> "Added " + added + " quiz questions to topic " + topicId);
        return new QuizGenerationResultDto(added, QuizPlan.QUESTIONS_PER_GENERATION, failures.size(),
                stats(topicId, userId), port.modelName(), QuizPrompt.VERSION, usedToday, config.aiDailyQuizLimit());
    }

    private QuizStatsDto stats(UUID topicId, Long userId) {
        Map<QuizDifficulty, Integer> c = questions.countByDifficulty(topicId, userId);
        int easy = c.get(QuizDifficulty.EASY);
        int medium = c.get(QuizDifficulty.MEDIUM);
        int hard = c.get(QuizDifficulty.HARD);
        return new QuizStatsDto(easy + medium + hard, easy, medium, hard, config.quizMaxPool(),
                QuizPlan.QUESTIONS_PER_GENERATION, PASS_RATIO);
    }

    private Topic ownedTopic(UUID topicId, Long userId) {
        return topicRepository.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new TopicNotFoundException(topicId.toString()));
    }

    private String noteText(UUID topicId, Long userId) {
        Note note = noteRepository.findByTopicIdAndUserId(topicId, userId).orElse(null);
        String text = note == null ? "" :
                NoteText.of(DocumentContainer.fromBytes(noteFileStorage.read(note.getContentPath())).contentJson());
        if (text.isBlank()) {
            throw new ValidationException("Najpierw zapisz treść notatki — quiz powstaje na jej podstawie.");
        }
        return text;
    }

    /** Najnowsze pytania tego samego poziomu — model dostaje je jako listę "nie powtarzaj". */
    private static List<String> avoidStems(List<QuizQuestion> existing, QuizDifficulty difficulty) {
        return existing.stream()
                .filter(q -> q.difficulty() == difficulty)
                .sorted(Comparator.comparing(QuizQuestion::createdAt).reversed())
                .limit(QuizPrompt.MAX_AVOID)
                .map(QuizQuestion::question)
                .toList();
    }

    private static java.util.concurrent.ThreadFactory daemonThreads() {
        return r -> {
            Thread t = new Thread(r, "quiz-ai");
            t.setDaemon(true);
            return t;
        };
    }
}
