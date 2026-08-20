package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.domain.topic.dto.UpdateTopicRequest;
import com.example.knowledgeforge.support.ServiceHarness;
import com.example.knowledgeforge.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy współbieżności TopicService — bez Thread.sleep: startuje wątki razem przez CountDownLatch,
 * a nie przez czasowe zgadywanie. Baza to osobny schemat testowy (zob. ServiceHarness/TestDatabase).
 */
class TopicServiceConcurrencyTest {

    ServiceHarness harness;
    long userId;
    UUID categoryId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        harness = ServiceHarness.withFreshUser(tempDir, "topic-concurrency");
        userId = harness.currentUser.id();
        categoryId = TestFixtures.insertCategory(harness.dataSource, userId, "Concurrency test category");
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    @Test
    void concurrent_topic_creation_in_same_category_ends_with_correct_total_count() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<TopicDto>> tasks = IntStream.range(0, threads)
                .<Callable<TopicDto>>mapToObj(i -> () -> {
                    ready.countDown();
                    start.await();
                    CreateTopicRequest req = new CreateTopicRequest(
                            categoryId, "Concurrent topic " + i, null, null, DetailLevel.MEDIUM, TopicType.NOTE);
                    return harness.topicService.create(req, "client-" + i);
                })
                .collect(Collectors.toList());

        List<Future<TopicDto>> futures = tasks.stream().map(pool::submit).collect(Collectors.toList());
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        List<TopicDto> results = new java.util.ArrayList<>();
        for (Future<TopicDto> f : futures) {
            results.add(f.get(10, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(threads, results.size());
        // Wszystkie tematy naprawdę zapisane, żaden request się nie wywalił.
        try (Connection con = harness.dataSource.getConnection()) {
            int finalCount = harness.topicDao.countDirectByCategoryId(con, categoryId, userId);
            assertEquals(threads, finalCount, "final DB count must reflect every concurrently created topic");
        }
        // Każda odpowiedź niesie SWÓJ własny, poprawny (>=1) licznik zwrócony w tej samej transakcji
        // co insert — nie musi być globalnie unikalny/monotoniczny między wątkami (dwie transakcje
        // mogą nie widzieć się nawzajem pod READ COMMITTED), ale musi być w granicach [1, threads].
        assertTrue(results.stream().allMatch(r -> r.getCategoryTopicCount() != null
                && r.getCategoryTopicCount() >= 1 && r.getCategoryTopicCount() <= threads));
    }

    @Test
    void concurrent_updates_with_same_expected_version_one_wins_one_conflicts() throws Exception {
        CreateTopicRequest createReq = new CreateTopicRequest(
                categoryId, "Original title", null, null, DetailLevel.MEDIUM, TopicType.NOTE);
        TopicDto created = harness.topicService.create(createReq, "creator");
        UUID topicId = created.getId();
        int baseVersion = created.getVersion();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Callable<Void>> tasks = IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    ready.countDown();
                    start.await();
                    UpdateTopicRequest req = new UpdateTopicRequest(
                            "Title from thread " + i, null, null, null, null, baseVersion);
                    try {
                        harness.topicService.update(topicId, req, "client-" + i);
                        successCount.incrementAndGet();
                    } catch (ConflictException e) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                })
                .collect(Collectors.toList());

        List<Future<Void>> futures = tasks.stream().map(pool::submit).collect(Collectors.toList());
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<Void> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successCount.get(), "exactly one concurrent update with the same expectedVersion must win");
        assertEquals(1, conflictCount.get(), "exactly one concurrent update with the same expectedVersion must conflict");

        TopicDto finalTopic = harness.topicService.getById(topicId);
        assertEquals(baseVersion + 1, finalTopic.getVersion(), "version must have advanced exactly once");
    }

    @Test
    void operations_on_different_topics_do_not_block_each_other() throws Exception {
        CreateTopicRequest reqA = new CreateTopicRequest(
                categoryId, "Topic A", null, null, DetailLevel.MEDIUM, TopicType.NOTE);
        CreateTopicRequest reqB = new CreateTopicRequest(
                categoryId, "Topic B", null, null, DetailLevel.MEDIUM, TopicType.NOTE);
        TopicDto a = harness.topicService.create(reqA, "creator");
        TopicDto b = harness.topicService.create(reqB, "creator");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Long> updateA = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.topicService.update(a.getId(),
                    new UpdateTopicRequest("Topic A renamed", null, null, null, null, a.getVersion()), "c1");
            return System.nanoTime() - t0;
        };
        Callable<Long> updateB = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.topicService.update(b.getId(),
                    new UpdateTopicRequest("Topic B renamed", null, null, null, null, b.getVersion()), "c2");
            return System.nanoTime() - t0;
        };

        Future<Long> fa = pool.submit(updateA);
        Future<Long> fb = pool.submit(updateB);
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        long durationA = fa.get(10, TimeUnit.SECONDS);
        long durationB = fb.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Best-effort: bez żadnego locka na innym zasobie, obie aktualizacje powinny zmieścić się
        // w rozsądnym czasie (nie sekundy) — nie sprawdzamy dokładnej kolejności, tylko brak
        // wielosekundowego oczekiwania, które świadczyłoby o niepotrzebnej serializacji.
        assertTrue(durationA < TimeUnit.SECONDS.toNanos(5), "update on topic A should not be blocked by topic B");
        assertTrue(durationB < TimeUnit.SECONDS.toNanos(5), "update on topic B should not be blocked by topic A");
    }
}
