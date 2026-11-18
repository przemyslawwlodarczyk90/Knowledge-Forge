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
import com.example.knowledgeforge.json.JsonMapper;
import com.example.knowledgeforge.quiz.dto.GenerateQuizRequest;
import com.example.knowledgeforge.quiz.dto.QuizGenerationResultDto;
import com.example.knowledgeforge.repository.NoteRepository;
import com.example.knowledgeforge.repository.TopicRepository;
import com.example.knowledgeforge.service.CurrentUser;
import com.example.knowledgeforge.storage.NoteFileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orkiestracja generowania (równoległe porcje, częściowe awarie, limity) na sztucznym porcie AI. */
class QuizServiceTest {

    static final long USER = 7L;
    static final UUID TOPIC = UUID.randomUUID();

    TopicRepository topics = mock(TopicRepository.class);
    NoteRepository notes = mock(NoteRepository.class);
    NoteFileStorage storage = mock(NoteFileStorage.class);
    QuizQuestionRepository repo = mock(QuizQuestionRepository.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    AiUsageGuard guard = mock(AiUsageGuard.class);
    ExecutorService executor = Executors.newFixedThreadPool(6);
    Map<Integer, QuizChunk> chunks = new ConcurrentHashMap<>();
    Function<QuizChunk, QuizBatch> model;

    QuizGenerationPort port = new QuizGenerationPort() {
        @Override
        public QuizBatch generate(QuizChunk chunk) {
            chunks.put(chunk.index(), chunk);
            return model.apply(chunk);
        }

        @Override
        public String modelName() {
            return "fake-model";
        }
    };

    QuizService service(int maxPool) {
        Properties p = new Properties();
        p.setProperty("ai.quiz-max-pool", String.valueOf(maxPool));
        p.setProperty("ai.daily-quiz-limit", "5");
        p.setProperty("ai.quiz-timeout-seconds", "5");
        return new QuizService(topics, notes, storage, repo, currentUser, guard, port, AppConfig.fromProperties(p),
                Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"), ZoneOffset.UTC),
                new QuizDraftValidator(new Random(1)), executor);
    }

    /** Model odpowiadający tyloma unikalnymi, poprawnymi pytaniami, o ile prosi porcja. */
    static QuizBatch answering(QuizChunk c) {
        List<QuizBatch.Item> items = new ArrayList<>();
        for (int i = 0; i < c.count(); i++) {
            items.add(new QuizBatch.Item(c.difficulty(), "Pytanie " + c.index() + "-" + i, null,
                    List.of("a", "b", "c", "d"), i % 4, "wyjaśnienie"));
        }
        return new QuizBatch(items);
    }

    @BeforeEach
    void setUp() throws Exception {
        when(currentUser.id()).thenReturn(USER);
        Topic topic = new Topic();
        topic.setTitle("Rekurencja");
        when(topics.findByIdAndUserId(TOPIC, USER)).thenReturn(Optional.of(topic));
        Note note = new Note();
        note.setContentPath("/notes/x.kfdoc");
        when(notes.findByTopicIdAndUserId(TOPIC, USER)).thenReturn(Optional.of(note));
        var doc = JsonMapper.get().readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Warunek stopu"}]}]}""");
        when(storage.read("/notes/x.kfdoc")).thenReturn(DocumentContainer.of(doc, Map.of()).toBytes());
        when(guard.checkAndIncrementQuiz(USER)).thenReturn(1);
        when(repo.findByTopic(TOPIC, USER)).thenReturn(List.of());
        when(repo.countByDifficulty(TOPIC, USER)).thenReturn(counts(30, 40, 30));
        model = QuizServiceTest::answering;
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    static Map<QuizDifficulty, Integer> counts(int easy, int medium, int hard) {
        Map<QuizDifficulty, Integer> m = new EnumMap<>(QuizDifficulty.class);
        m.put(QuizDifficulty.EASY, easy);
        m.put(QuizDifficulty.MEDIUM, medium);
        m.put(QuizDifficulty.HARD, hard);
        return m;
    }

    @SuppressWarnings("unchecked")
    List<QuizQuestion> inserted() {
        ArgumentCaptor<List<QuizQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo).insertAll(eq(USER), captor.capture());
        return captor.getValue();
    }

    @Test
    void generates_hundred_questions_in_six_parallel_chunks_from_the_note_text() {
        QuizGenerationResultDto result = service(1000).generate(TOPIC, new GenerateQuizRequest("więcej o stosie"));

        assertThat(result.added()).isEqualTo(100);
        assertThat(result.requested()).isEqualTo(100);
        assertThat(result.failedChunks()).isZero();
        assertThat(result.model()).isEqualTo("fake-model");
        assertThat(result.promptVersion()).isEqualTo(QuizPrompt.VERSION);
        assertThat(result.dailyLimit()).isEqualTo(5);
        assertThat(result.stats().total()).isEqualTo(100);
        assertThat(chunks).hasSize(6);
        assertThat(chunks.values()).allSatisfy(c -> {
            assertThat(c.noteText()).isEqualTo("Warunek stopu");
            assertThat(c.userInstruction()).isEqualTo("więcej o stosie");
            assertThat(c.topicTitle()).isEqualTo("Rekurencja");
        });
        List<QuizQuestion> saved = inserted();
        assertThat(saved).hasSize(100);
        assertThat(saved.stream().filter(q -> q.difficulty() == QuizDifficulty.HARD)).hasSize(30);
    }

    @Test
    void failed_chunks_do_not_discard_the_successful_ones() {
        model = c -> {
            if (c.index() == 1 || c.index() == 4) throw new AiResponseException("zła odpowiedź");
            return answering(c);
        };

        QuizGenerationResultDto result = service(1000).generate(TOPIC, null);

        assertThat(result.failedChunks()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(100 - 10 - 20);
        assertThat(chunks.get(0).userInstruction()).isNull();
    }

    @Test
    void when_every_chunk_fails_the_error_is_reported_and_nothing_is_saved() {
        model = c -> { throw new AiResponseException("x"); };
        assertThatThrownBy(() -> service(1000).generate(TOPIC, null))
                .isInstanceOf(AiResponseException.class).hasMessageContaining("żadnego poprawnego");

        model = c -> { throw new AiNotConfiguredException("brak klucza"); };
        assertThatThrownBy(() -> service(1000).generate(TOPIC, null))
                .isInstanceOf(AiNotConfiguredException.class).hasMessage("brak klucza");

        model = c -> new QuizBatch(List.of(new QuizBatch.Item(c.difficulty(), "q", null, List.of("a"), 0, "e")));
        assertThatThrownBy(() -> service(1000).generate(TOPIC, null)).isInstanceOf(AiResponseException.class);
        verify(repo, never()).insertAll(anyLong(), any());
    }

    @Test
    void existing_pool_is_sent_as_avoid_list_and_its_duplicates_are_skipped() {
        Instant t = Instant.parse("2029-12-01T10:00:00Z");
        List<QuizQuestion> pool = List.of(
                new QuizQuestion(UUID.randomUUID(), TOPIC, QuizDifficulty.EASY, "Pytanie 0-0", null,
                        List.of("a", "b", "c", "d"), 0, "e", t),
                new QuizQuestion(UUID.randomUUID(), TOPIC, QuizDifficulty.HARD, "Trudne stare", null,
                        List.of("a", "b", "c", "d"), 0, "e", t.plusSeconds(5)));
        when(repo.findByTopic(TOPIC, USER)).thenReturn(pool);

        QuizGenerationResultDto result = service(1000).generate(TOPIC, null);

        assertThat(result.added()).isEqualTo(99);
        assertThat(chunks.get(0).avoid()).containsExactly("Pytanie 0-0");
        assertThat(chunks.get(4).avoid()).containsExactly("Trudne stare");
        assertThat(chunks.get(2).avoid()).isEmpty();
    }

    @Test
    void pool_limit_blocks_generation_or_caps_the_added_questions() {
        List<QuizQuestion> pool = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            pool.add(new QuizQuestion(UUID.randomUUID(), TOPIC, QuizDifficulty.MEDIUM, "stare " + i, null,
                    List.of("a", "b", "c", "d"), 0, "e", Instant.EPOCH));
        }
        when(repo.findByTopic(TOPIC, USER)).thenReturn(pool);

        assertThatThrownBy(() -> service(150).generate(TOPIC, null)).isInstanceOf(ConflictException.class);
        verify(guard, never()).checkAndIncrementQuiz(anyLong());

        assertThat(service(200).generate(TOPIC, null).added()).isEqualTo(50);
    }

    @Test
    void requires_a_saved_non_empty_note() throws Exception {
        when(notes.findByTopicIdAndUserId(TOPIC, USER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service(1000).generate(TOPIC, null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("zapisz treść notatki");

        Note empty = new Note();
        empty.setContentPath("/notes/empty.kfdoc");
        when(notes.findByTopicIdAndUserId(TOPIC, USER)).thenReturn(Optional.of(empty));
        when(storage.read("/notes/empty.kfdoc")).thenReturn(
                DocumentContainer.of(JsonMapper.get().readTree("{\"type\":\"doc\",\"content\":[]}"), Map.of()).toBytes());
        assertThatThrownBy(() -> service(1000).generate(TOPIC, null)).isInstanceOf(ValidationException.class);
        verify(guard, never()).checkAndIncrementQuiz(anyLong());
    }

    @Test
    void draw_validates_count_and_returns_fresh_random_sample_with_pass_ratio() {
        QuizQuestion q = new QuizQuestion(UUID.randomUUID(), TOPIC, QuizDifficulty.EASY, "q", "code",
                List.of("a", "b", "c", "d"), 2, "e", Instant.EPOCH);
        when(repo.drawRandom(TOPIC, USER, 20)).thenReturn(List.of(q));
        QuizService s = service(1000);

        var quiz = s.draw(TOPIC, null);
        assertThat(quiz.questions()).hasSize(1);
        assertThat(quiz.questions().get(0).correctIndex()).isEqualTo(2);
        assertThat(quiz.questions().get(0).code()).isEqualTo("code");
        assertThat(quiz.poolSize()).isEqualTo(100);
        assertThat(quiz.passRatio()).isEqualTo(0.75);

        assertThatThrownBy(() -> s.draw(TOPIC, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> s.draw(TOPIC, 51)).isInstanceOf(ValidationException.class);
        verify(repo, never()).drawRandom(any(), anyLong(), eq(0));
    }

    @Test
    void stats_clear_and_ownership() {
        QuizService s = service(1000);
        var stats = s.stats(TOPIC);
        assertThat(stats.total()).isEqualTo(100);
        assertThat(stats.easy()).isEqualTo(30);
        assertThat(stats.medium()).isEqualTo(40);
        assertThat(stats.hard()).isEqualTo(30);
        assertThat(stats.maxPool()).isEqualTo(1000);
        assertThat(stats.questionsPerGeneration()).isEqualTo(100);

        when(repo.deleteByTopic(TOPIC, USER)).thenReturn(100);
        when(repo.countByDifficulty(TOPIC, USER)).thenReturn(counts(0, 0, 0));
        assertThat(s.clear(TOPIC).total()).isZero();

        UUID foreign = UUID.randomUUID();
        when(topics.findByIdAndUserId(foreign, USER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> s.stats(foreign)).isInstanceOf(TopicNotFoundException.class);
        assertThatThrownBy(() -> s.draw(foreign, 5)).isInstanceOf(TopicNotFoundException.class);
        assertThatThrownBy(() -> s.generate(foreign, null)).isInstanceOf(TopicNotFoundException.class);
        verify(repo, never()).drawRandom(eq(foreign), anyLong(), anyInt());
        s.shutdown();
        assertThat(Set.of(executor.isShutdown())).containsExactly(true);
    }
}
