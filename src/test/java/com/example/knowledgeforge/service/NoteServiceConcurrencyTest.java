package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.note.dto.NoteDto;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.json.JsonMapper;
import com.example.knowledgeforge.support.ServiceHarness;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy współbieżności NoteService — optimistic locking na note.version, bez Thread.sleep
 * (CountDownLatch startuje wątki razem).
 */
class NoteServiceConcurrencyTest {

    ServiceHarness harness;
    long userId;
    UUID categoryId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        harness = ServiceHarness.withFreshUser(tempDir, "note-concurrency");
        userId = harness.currentUser.id();
        categoryId = com.example.knowledgeforge.support.TestFixtures.insertCategory(
                harness.dataSource, userId, "Note concurrency category");
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    private UUID createTopic(String title) {
        TopicDto topic = harness.topicService.create(
                new CreateTopicRequest(categoryId, title, null, null, DetailLevel.MEDIUM, TopicType.NOTE),
                "creator");
        return topic.getId();
    }

    private static SaveNoteRequest contentRequest(String text, Integer baseVersion) {
        ObjectNode content = JsonMapper.get().createObjectNode();
        content.put("type", "doc");
        content.put("text", text);
        return new SaveNoteRequest(content, List.of(), baseVersion);
    }

    @Test
    void concurrent_saves_of_the_same_note_version_one_succeeds_one_conflicts() throws Exception {
        UUID topicId = createTopic("Note race topic");
        // Pierwszy zapis tworzy notatkę w wersji 1 — obaj wątki wystartują z baseVersion=1.
        NoteDto initial = harness.noteService.save(topicId, contentRequest("initial", null), "creator");
        assertEquals(1, initial.getVersion());

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Callable<Void>> tasks = List.of(
                () -> {
                    ready.countDown();
                    start.await();
                    try {
                        harness.noteService.save(topicId, contentRequest("from thread A", 1), "c1");
                        successCount.incrementAndGet();
                    } catch (ConflictException e) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                },
                () -> {
                    ready.countDown();
                    start.await();
                    try {
                        harness.noteService.save(topicId, contentRequest("from thread B", 1), "c2");
                        successCount.incrementAndGet();
                    } catch (ConflictException e) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                });

        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<Void> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successCount.get(), "exactly one concurrent save at the same baseVersion must win");
        assertEquals(1, conflictCount.get(), "exactly one concurrent save at the same baseVersion must conflict");

        NoteDto finalNote = harness.noteService.getByTopicId(topicId);
        assertEquals(2, finalNote.getVersion(), "version must have advanced exactly once past the race");
    }

    @Test
    void saves_on_different_topics_do_not_block_each_other() throws Exception {
        UUID topicA = createTopic("Topic A");
        UUID topicB = createTopic("Topic B");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Long> saveA = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.noteService.save(topicA, contentRequest("content A", null), "c1");
            return System.nanoTime() - t0;
        };
        Callable<Long> saveB = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.noteService.save(topicB, contentRequest("content B", null), "c2");
            return System.nanoTime() - t0;
        };

        Future<Long> fa = pool.submit(saveA);
        Future<Long> fb = pool.submit(saveB);
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        long durationA = fa.get(10, TimeUnit.SECONDS);
        long durationB = fb.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(durationA < TimeUnit.SECONDS.toNanos(5), "save on topic A should not be blocked by topic B");
        assertTrue(durationB < TimeUnit.SECONDS.toNanos(5), "save on topic B should not be blocked by topic A");

        assertEquals(1, harness.noteService.getByTopicId(topicA).getVersion());
        assertEquals(1, harness.noteService.getByTopicId(topicB).getVersion());
    }
}
