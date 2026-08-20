package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.attachment.dto.AttachmentDto;
import com.example.knowledgeforge.domain.exception.AttachmentNotFoundException;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.support.ServiceHarness;
import com.example.knowledgeforge.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
 * Testy współbieżności AttachmentService#delete — zob. AttachmentLockRegistry (per-attachmentId,
 * nie globalny lock). Bez Thread.sleep (CountDownLatch startuje wątki razem).
 */
class AttachmentServiceConcurrencyTest {

    ServiceHarness harness;
    UUID topicId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        harness = ServiceHarness.withFreshUser(tempDir, "attachment-concurrency");
        long userId = harness.currentUser.id();
        UUID categoryId = TestFixtures.insertCategory(harness.dataSource, userId, "Attachment concurrency category");
        TopicDto topic = harness.topicService.create(
                new CreateTopicRequest(categoryId, "Attachment race topic", null, null, DetailLevel.MEDIUM, TopicType.NOTE),
                "creator");
        topicId = topic.getId();
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    @Test
    void concurrent_delete_of_the_same_attachment_one_succeeds_one_gets_not_found() throws Exception {
        AttachmentDto uploaded = harness.attachmentService.upload(
                topicId, "race.txt", "text/plain",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), null, "creator");

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger notFoundCount = new AtomicInteger();

        Callable<Void> deleteTask = () -> {
            ready.countDown();
            start.await();
            try {
                harness.attachmentService.delete(uploaded.id(), "client");
                successCount.incrementAndGet();
            } catch (AttachmentNotFoundException e) {
                notFoundCount.incrementAndGet();
            }
            return null;
        };

        List<Future<Void>> futures = List.of(pool.submit(deleteTask), pool.submit(deleteTask));
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<Void> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Blokada per-attachmentId serializuje obie sekcje krytyczne: pierwsza faktycznie usuwa,
        // druga (po zwolnieniu locka) już nie znajduje rekordu -> AttachmentNotFoundException,
        // nigdy nie rzuca czegoś niespodziewanego ani nie kasuje "podwójnie" tego samego pliku.
        assertEquals(1, successCount.get(), "exactly one concurrent delete of the same attachment must succeed");
        assertEquals(1, notFoundCount.get(), "the loser must get a graceful not-found, not a crash");
    }

    @Test
    void deletes_of_different_attachments_do_not_block_each_other() throws Exception {
        AttachmentDto a = harness.attachmentService.upload(
                topicId, "a.txt", "text/plain",
                new ByteArrayInputStream("A".getBytes(StandardCharsets.UTF_8)), null, "creator");
        AttachmentDto b = harness.attachmentService.upload(
                topicId, "b.txt", "text/plain",
                new ByteArrayInputStream("B".getBytes(StandardCharsets.UTF_8)), null, "creator");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Long> deleteA = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.attachmentService.delete(a.id(), "c1");
            return System.nanoTime() - t0;
        };
        Callable<Long> deleteB = () -> {
            ready.countDown();
            start.await();
            long t0 = System.nanoTime();
            harness.attachmentService.delete(b.id(), "c2");
            return System.nanoTime() - t0;
        };

        Future<Long> fa = pool.submit(deleteA);
        Future<Long> fb = pool.submit(deleteB);
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        long durationA = fa.get(10, TimeUnit.SECONDS);
        long durationB = fb.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(durationA < TimeUnit.SECONDS.toNanos(5), "delete of attachment A should not be blocked by B");
        assertTrue(durationB < TimeUnit.SECONDS.toNanos(5), "delete of attachment B should not be blocked by A");
    }
}
