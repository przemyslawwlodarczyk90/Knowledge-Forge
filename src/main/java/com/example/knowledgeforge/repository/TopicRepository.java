package com.example.knowledgeforge.repository;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.TopicType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Dostęp do tabeli topic (dawne TopicDao). Warunkowe aktualizacje z optimistic lockingiem
 * (WHERE version = :expectedVersion, version = version + 1 w tym samym UPDATE) są jawnymi
 * zapytaniami @Modifying — NIE używamy @Version Hibernate'a, bo wersję bumpują też operacje
 * zbiorcze (sweep aktualności) i zapis notatki, dokładnie tak jak przed migracją.
 *
 * Wołane wewnątrz TransactionTemplate serwisu dołączają do jego transakcji; wołane samodzielnie
 * wykonują się we własnej, krótkiej transakcji (odpowiednik dawnego autocommitu).
 */
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findByIdAndUserId(UUID id, Long userId);

    /** Wszystkie tematy/wpisy użytkownika, niezależnie od kategorii — używane przez wyszukiwarkę. */
    List<Topic> findAllByUserId(Long userId);

    List<Topic> findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, UUID categoryId);

    long countByUserIdAndCategoryId(Long userId, UUID categoryId);

    boolean existsByUserIdAndCategoryId(Long userId, UUID categoryId);

    @Query("select t.categoryId, count(t) from Topic t where t.userId = :userId group by t.categoryId")
    List<Object[]> countGroupedByCategory(@Param("userId") Long userId);

    @Query("select t.categoryId, count(t) from Topic t where t.userId = :userId and t.detailLevel = :level group by t.categoryId")
    List<Object[]> countGroupedByCategoryWithDetailLevel(@Param("userId") Long userId, @Param("level") DetailLevel level);

    /** Autorzy użyci choć raz — do rozwijanej listy w filtrach. */
    @Query("select distinct t.author from Topic t where t.userId = :userId and t.author is not null and t.author <> '' order by t.author")
    List<String> findDistinctAuthors(@Param("userId") Long userId);

    /** Liczba tematów na kategorię (bezpośrednio w niej, bez podkategorii) — do cyferki w drzewie. */
    default Map<UUID, Integer> countByCategoryForUser(Long userId) {
        return toCountMap(countGroupedByCategory(userId));
    }

    /** Liczba tematów niskiego poziomu szczegółowości na kategorię — domyślnie ukryte w drzewie. */
    default Map<UUID, Integer> countLowDetailByCategoryForUser(Long userId) {
        return toCountMap(countGroupedByCategoryWithDetailLevel(userId, DetailLevel.LOW));
    }

    /**
     * Bezpośrednia (bez podkategorii) liczba tematów pojedynczej kategorii. Wołana w TEJ SAMEJ
     * transakcji co insert nowego tematu (zob. TopicService#create) widzi własny, jeszcze
     * niezacommitowany wiersz — Hibernate flushuje insert przed zapytaniem (FlushMode.AUTO).
     */
    default int countDirectByCategoryId(UUID categoryId, Long userId) {
        return (int) countByUserIdAndCategoryId(userId, categoryId);
    }

    /** INSERT nowego tematu — te same wartości domyślne, co dawne TopicDao#insert. */
    default Topic insert(Topic topic) {
        if (topic.getId() == null) topic.setId(UUID.randomUUID());
        Instant now = Instant.now();
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        if (topic.getStatus() == null) topic.setStatus(TopicStatus.NEW);
        if (topic.getVersion() == null) topic.setVersion(1);
        Topic saved = save(topic);
        Logger.getLogger(TopicRepository.class.getName()).fine(() -> "Inserted topic " + saved.getId());
        return saved;
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Topic t set t.title = :title, t.shortPrompt = :shortPrompt, t.author = :author,
                               t.detailLevel = :detailLevel, t.type = :type, t.status = :status,
                               t.updatedAt = :updatedAt, t.version = t.version + 1
            where t.id = :id and t.version = :expectedVersion
            """)
    int updateFieldsWithVersionCheck(@Param("id") UUID id, @Param("title") String title,
                                     @Param("shortPrompt") String shortPrompt, @Param("author") String author,
                                     @Param("detailLevel") DetailLevel detailLevel, @Param("type") TopicType type,
                                     @Param("status") TopicStatus status, @Param("updatedAt") Instant updatedAt,
                                     @Param("expectedVersion") int expectedVersion);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Topic t set t.title = :title, t.shortPrompt = :shortPrompt, t.author = :author,
                               t.detailLevel = :detailLevel, t.type = :type, t.status = :status,
                               t.updatedAt = :updatedAt, t.version = t.version + 1
            where t.id = :id
            """)
    int updateFields(@Param("id") UUID id, @Param("title") String title,
                     @Param("shortPrompt") String shortPrompt, @Param("author") String author,
                     @Param("detailLevel") DetailLevel detailLevel, @Param("type") TopicType type,
                     @Param("status") TopicStatus status, @Param("updatedAt") Instant updatedAt);

    /**
     * Optimistic locking: gdy expectedVersion != null, aktualizacja jest warunkowa
     * (WHERE version = ?) i version jest atomowo inkrementowana w tym samym UPDATE.
     * 0 zmienionych wierszy = ktoś inny zapisał nowszą wersję w międzyczasie -> ConflictException (409).
     * expectedVersion == null = wywołanie bezwarunkowe (zgodność wsteczna) — version i tak rośnie.
     */
    default Topic update(Topic topic, Integer expectedVersion) {
        Logger log = Logger.getLogger(TopicRepository.class.getName());
        topic.setUpdatedAt(Instant.now());
        int affected = expectedVersion != null
                ? updateFieldsWithVersionCheck(topic.getId(), topic.getTitle(), topic.getShortPrompt(), topic.getAuthor(),
                        topic.getDetailLevel(), topic.getType(), topic.getStatus(), topic.getUpdatedAt(), expectedVersion)
                : updateFields(topic.getId(), topic.getTitle(), topic.getShortPrompt(), topic.getAuthor(),
                        topic.getDetailLevel(), topic.getType(), topic.getStatus(), topic.getUpdatedAt());
        if (affected == 0) {
            log.warning(() -> "Optimistic lock conflict updating topic " + topic.getId()
                    + " (expectedVersion=" + expectedVersion + ")");
            throw new ConflictException("Topic was modified by someone else in the meantime");
        }
        topic.setVersion(expectedVersion != null ? expectedVersion + 1 : topic.getVersion() + 1);
        log.fine(() -> "Updated topic " + topic.getId() + " -> version=" + topic.getVersion());
        return topic;
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Topic t where t.id = :id")
    int deleteRowById(@Param("id") UUID id);

    // ── Weryfikacja aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) ──────────────────────────────

    /**
     * Ręczne potwierdzenie aktualności — WYŁĄCZNIE pola aktualności, status/treść tematu nietknięte.
     * Optimistic locking jak w #update. 0 zmienionych wierszy = konflikt wersji.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Topic t set t.actualityVerified = true, t.lastVerificationOfActualityDate = :verifiedAt,
                               t.version = t.version + 1
            where t.id = :id and t.version = :expectedVersion
            """)
    int confirmActuality(@Param("id") UUID topicId, @Param("expectedVersion") int expectedVersion,
                         @Param("verifiedAt") Instant verifiedAt);

    /**
     * Woła NoteService#save po udanym zapisie treści — jeden atomowy UPDATE łączący ewentualne
     * przejście statusu NEW -> NOTE_ADDED z potwierdzeniem aktualności, żeby udany zapis notatki
     * bumpował topic.version DOKŁADNIE RAZ. Optimistic locking jak wyżej.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Topic t set t.status = :status, t.actualityVerified = true,
                               t.lastVerificationOfActualityDate = :verifiedAt, t.version = t.version + 1
            where t.id = :id and t.version = :expectedVersion
            """)
    int markNoteSaved(@Param("id") UUID topicId, @Param("status") TopicStatus status,
                      @Param("expectedVersion") int expectedVersion, @Param("verifiedAt") Instant verifiedAt);

    /**
     * Scheduler — jeden zbiorczy, warunkowy UPDATE (wszyscy użytkownicy) zamiast wczytywania
     * wszystkich tematów do Javy. Rekord traci aktualność, gdy jego data bazowa (ostatnia
     * weryfikacja, a w jej braku createdAt) jest nie później niż `cutoff`. Dotyka WYŁĄCZNIE
     * rekordów wciąż aktualnych; version rośnie atomowo. RETURNING zwraca pełne wiersze —
     * potrzebne do rozgłoszenia zdarzeń WebSocket per temat (i per właściciel).
     */
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE topic SET actuality_verified = FALSE, version = version + 1
            WHERE actuality_verified = TRUE AND COALESCE(last_verification_of_actuality_date, created_at) <= :cutoff
            RETURNING *
            """)
    List<Topic> markStaleAsUnverified(@Param("cutoff") Instant cutoff);

    @Query("""
            select t from Topic t where t.userId = :userId and t.actualityVerified = false
            order by coalesce(t.lastVerificationOfActualityDate, t.createdAt) asc
            """)
    List<Topic> findForActualityReviewAllAuthors(@Param("userId") Long userId);

    @Query("""
            select t from Topic t where t.userId = :userId and t.actualityVerified = false and t.author = :author
            order by coalesce(t.lastVerificationOfActualityDate, t.createdAt) asc
            """)
    List<Topic> findForActualityReviewByAuthor(@Param("userId") Long userId, @Param("author") String author);

    /**
     * Lista tematów oczekujących na sprawdzenie (GET /api/topics/actuality-review) — wyłącznie
     * actualityVerified = false, opcjonalnie zawężone po autorze, od najstarszej daty bazowej.
     */
    default List<Topic> findForActualityReview(Long userId, String author) {
        return author == null
                ? findForActualityReviewAllAuthors(userId)
                : findForActualityReviewByAuthor(userId, author);
    }

    private static Map<UUID, Integer> toCountMap(List<Object[]> rows) {
        Map<UUID, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }
}
