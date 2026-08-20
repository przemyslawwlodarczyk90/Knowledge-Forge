package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.exception.ConflictException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Blokada per-zasób (per attachmentId) — NIE jeden globalny lock na wszystkie załączniki.
 * Używana wyłącznie wokół krótkiej sekcji krytycznej usuwania załącznika (zob. AttachmentService#delete).
 */
// MULTI-THREADING:
// ConcurrentHashMap<Long, ReentrantLock> — jedna blokada na jeden attachmentId. Operacje na
// RÓŻNYCH załącznikach nigdy się nie blokują nawzajem (blokada dotyczy tylko id, którego
// operacja faktycznie dotyczy). Chroni krótką sekcję: odczyt rekordu + usunięcie pliku z dysku +
// usunięcie rekordu z bazy dla TEGO SAMEGO id, żeby dwa równoczesne DELETE na ten sam załącznik
// nie zalogowały sprzecznych/mylących wyników (sam DELETE w Postgresie i tak jest atomowy, a
// Files.deleteIfExists jest idempotentny — ten lock to dodatkowa, deterministyczna kolejność,
// nie jedyna linia obrony). Mapa NIE rośnie trwale: po zwolnieniu locka wpis jest od razu usuwany
// przez remove(id, tenSamObiektLocka) — wariant dwuargumentowy usuwa wpis tylko wtedy, gdy wciąż
// wskazuje na TĘ SAMĄ instancję (bezpieczne pod współbieżnością, bez okna na usunięcie cudzego,
// świeżo podstawionego locka). tryLock z timeoutem (nie lock() bez końca) — długo trwający upload
// NIGDY nie trzyma tego locka (upload zawsze tworzy świeży attachmentId, nie ma tu kolizji).
public class AttachmentLockRegistry {

    private static final Logger log = Logger.getLogger(AttachmentLockRegistry.class.getName());
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(Long attachmentId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(attachmentId, id -> new ReentrantLock());
        boolean acquired;
        try {
            acquired = lock.tryLock(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException("Interrupted while waiting for attachment lock");
        }
        if (!acquired) {
            log.warning(() -> "Timed out waiting for lock on attachment " + attachmentId);
            throw new ConflictException("Another operation on this attachment is already in progress");
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
            locks.remove(attachmentId, lock);
        }
    }
}
