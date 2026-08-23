package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.Topic;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.TopicType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TopicDao {

    private static final Logger log = Logger.getLogger(TopicDao.class.getName());

    private final DataSource dataSource;

    public TopicDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Topic> findByIdAndUserId(UUID id, Long userId) {
        try (Connection con = dataSource.getConnection()) {
            return findByIdAndUserId(con, id, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query topic by id", e);
        }
    }

    /** Wariant na przekazanym połączeniu — do użycia wewnątrz szerszej transakcji (zob. NoteService#save). */
    public Optional<Topic> findByIdAndUserId(Connection con, UUID id, Long userId) {
        String sql = "SELECT * FROM topic WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query topic by id", e);
        }
    }

    /** Wszystkie tematy/wpisy użytkownika, niezależnie od kategorii — używane przez wyszukiwarkę. */
    public List<Topic> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM topic WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all topics by user", e);
        }
    }

    public List<Topic> findAllByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, UUID categoryId) {
        String sql = "SELECT * FROM topic WHERE user_id = ? AND category_id = ? ORDER BY created_at DESC";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list topics by category", e);
        }
    }

    /** Liczba tematów na kategorię (bezpośrednio w niej, bez podkategorii) — do cyferki w drzewie. */
    public Map<UUID, Integer> countByCategoryForUser(Long userId) {
        String sql = "SELECT category_id, COUNT(*) AS cnt FROM topic WHERE user_id = ? GROUP BY category_id";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<UUID, Integer> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getObject("category_id", UUID.class), rs.getInt("cnt"));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count topics by category", e);
        }
    }

    /**
     * Bezpośrednia (bez podkategorii) liczba tematów pojedynczej kategorii — liczona przez
     * SELECT COUNT(*) w TEJ SAMEJ transakcji co insert nowego tematu (zob. TopicService#create),
     * więc od razu widzi własny, jeszcze niezacommitowany wiersz (ta sama sesja/transakcja).
     * To jedyne poprawne źródło "aktualnego licznika z bazy" po utworzeniu tematu — bez wyścigu
     * między "SELECT count" a "UPDATE count = count + 1" na współdzielonym liczniku (żadnego
     * takiego liczonika tu nie ma — COUNT(*) na tabeli topic jest zawsze źródłem prawdy).
     */
    public int countDirectByCategoryId(Connection con, UUID categoryId, Long userId) {
        String sql = "SELECT COUNT(*) FROM topic WHERE user_id = ? AND category_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count topics in category", e);
        }
    }

    /** Liczba tematów niskiego poziomu szczegółowości na kategorię (bezpośrednio w niej, bez podkategorii) —
     *  te tematy są domyślnie ukryte w drzewie, więc licznik w UI musi je umieć odjąć od sumy. */
    public Map<UUID, Integer> countLowDetailByCategoryForUser(Long userId) {
        String sql = "SELECT category_id, COUNT(*) AS cnt FROM topic WHERE user_id = ? AND detail_level = 'LOW' GROUP BY category_id";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<UUID, Integer> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getObject("category_id", UUID.class), rs.getInt("cnt"));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count low-detail topics by category", e);
        }
    }

    /** Autorzy użyci choć raz — do rozwijanej listy w filtrach. Autor dziś jest ręczny placeholder;
     *  docelowo będzie zczytywany z tokena, ale zapytanie już ma sens niezależnie od tego. */
    public List<String> findDistinctAuthors(Long userId) {
        String sql = "SELECT DISTINCT author FROM topic WHERE user_id = ? AND author IS NOT NULL AND author <> '' ORDER BY author";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) result.add(rs.getString("author"));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list distinct authors", e);
        }
    }

    public boolean existsByUserIdAndCategoryId(Long userId, UUID categoryId) {
        String sql = "SELECT 1 FROM topic WHERE user_id = ? AND category_id = ? LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check topics in category", e);
        }
    }

    public Topic insert(Topic topic) {
        try (Connection con = dataSource.getConnection()) {
            return insert(con, topic);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert topic", e);
        }
    }

    /** Wariant na przekazanym połączeniu — do użycia wewnątrz szerszej transakcji (zob. TopicService#create). */
    public Topic insert(Connection con, Topic topic) {
        if (topic.getId() == null) topic.setId(UUID.randomUUID());
        Instant now = Instant.now();
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        if (topic.getStatus() == null) topic.setStatus(TopicStatus.NEW);
        if (topic.getVersion() == null) topic.setVersion(1);

        String sql = """
                INSERT INTO topic (id, user_id, category_id, title, short_prompt, author, detail_level, type, status,
                                    created_at, updated_at, version, actuality_verified, last_verification_of_actuality_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, topic.getId());
            ps.setLong(2, topic.getUserId());
            ps.setObject(3, topic.getCategoryId());
            ps.setString(4, topic.getTitle());
            ps.setString(5, topic.getShortPrompt());
            ps.setString(6, topic.getAuthor());
            ps.setString(7, topic.getDetailLevel().name());
            ps.setString(8, topic.getType().name());
            ps.setString(9, topic.getStatus().name());
            ps.setTimestamp(10, Timestamp.from(topic.getCreatedAt()));
            ps.setTimestamp(11, Timestamp.from(topic.getUpdatedAt()));
            ps.setInt(12, topic.getVersion());
            // Nowo tworzony temat zawsze startuje jako aktualny, bez daty potwierdzenia — zob.
            // Topic#actualityVerified (domyślne pole Javy = true) i dokumentacja/ACTUALITY_VERIFICATION.txt.
            ps.setBoolean(13, topic.isActualityVerified());
            ps.setTimestamp(14, topic.getLastVerificationOfActualityDate() == null
                    ? null : Timestamp.from(topic.getLastVerificationOfActualityDate()));
            ps.executeUpdate();
            log.fine(() -> "Inserted topic " + topic.getId());
            return topic;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to insert topic " + topic.getId(), e);
            throw new RuntimeException("Failed to insert topic", e);
        }
    }

    /**
     * Optimistic locking: gdy expectedVersion != null, aktualizacja jest warunkowa
     * (WHERE version = ?) i version jest atomowo inkrementowana w tym samym UPDATE.
     * 0 zmienionych wierszy = ktoś inny zapisał nowszą wersję w międzyczasie -> ConflictException (409).
     * expectedVersion == null = wywołanie bezwarunkowe (zgodność wsteczna) — version i tak rośnie.
     */
    public Topic update(Topic topic, Integer expectedVersion) {
        topic.setUpdatedAt(Instant.now());
        String base = """
                UPDATE topic SET title = ?, short_prompt = ?, author = ?, detail_level = ?, type = ?, status = ?,
                                  updated_at = ?, version = version + 1
                WHERE id = ?
                """;
        String sql = expectedVersion != null ? base + " AND version = ?" : base;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, topic.getTitle());
            ps.setString(2, topic.getShortPrompt());
            ps.setString(3, topic.getAuthor());
            ps.setString(4, topic.getDetailLevel().name());
            ps.setString(5, topic.getType().name());
            ps.setString(6, topic.getStatus().name());
            ps.setTimestamp(7, Timestamp.from(topic.getUpdatedAt()));
            ps.setObject(8, topic.getId());
            if (expectedVersion != null) {
                ps.setInt(9, expectedVersion);
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                log.warning(() -> "Optimistic lock conflict updating topic " + topic.getId()
                        + " (expectedVersion=" + expectedVersion + ")");
                throw new ConflictException("Topic was modified by someone else in the meantime");
            }
            topic.setVersion(expectedVersion != null ? expectedVersion + 1 : topic.getVersion() + 1);
            log.fine(() -> "Updated topic " + topic.getId() + " -> version=" + topic.getVersion());
            return topic;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to update topic " + topic.getId(), e);
            throw new RuntimeException("Failed to update topic", e);
        }
    }

    /**
     * Wąski, wewnętrzny update statusu (np. NEW -> NOTE_ADDED po pierwszym zapisie notatki) —
     * na przekazanym połączeniu, wewnątrz cudzej transakcji (zob. NoteService#save). Zwraca
     * liczbę zmienionych wierszy zamiast rzucać wyjątek — wywołujący decyduje, czy zrobić rollback
     * całej (szerszej) transakcji.
     */
    public int updateStatus(Connection con, UUID topicId, TopicStatus status, int expectedVersion) {
        String sql = "UPDATE topic SET status = ?, updated_at = ?, version = version + 1 WHERE id = ? AND version = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setObject(3, topicId);
            ps.setInt(4, expectedVersion);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update topic status", e);
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM topic WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete topic", e);
        }
    }

    private Topic map(ResultSet rs) throws SQLException {
        Topic topic = new Topic();
        topic.setId(rs.getObject("id", UUID.class));
        topic.setUserId(rs.getLong("user_id"));
        topic.setCategoryId(rs.getObject("category_id", UUID.class));
        topic.setTitle(rs.getString("title"));
        topic.setShortPrompt(rs.getString("short_prompt"));
        topic.setAuthor(rs.getString("author"));
        topic.setDetailLevel(DetailLevel.valueOf(rs.getString("detail_level")));
        topic.setType(TopicType.valueOf(rs.getString("type")));
        topic.setStatus(TopicStatus.valueOf(rs.getString("status")));
        topic.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        topic.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        topic.setVersion(rs.getInt("version"));
        topic.setActualityVerified(rs.getBoolean("actuality_verified"));
        Timestamp lastVerification = rs.getTimestamp("last_verification_of_actuality_date");
        topic.setLastVerificationOfActualityDate(lastVerification == null ? null : lastVerification.toInstant());
        return topic;
    }

    // ── Weryfikacja aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) ──────────────────────────────

    /**
     * Ręczne potwierdzenie aktualności (POST .../verify-actuality) — WYŁĄCZNIE pola aktualności,
     * status/treść tematu nietknięte. Optimistic locking jak w #update: WHERE version = ?,
     * version rośnie atomowo w tym samym UPDATE. 0 zmienionych wierszy = konflikt wersji.
     */
    public int confirmActuality(Connection con, UUID topicId, int expectedVersion, Instant verifiedAt) {
        String sql = """
                UPDATE topic SET actuality_verified = TRUE, last_verification_of_actuality_date = ?, version = version + 1
                WHERE id = ? AND version = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(verifiedAt));
            ps.setObject(2, topicId);
            ps.setInt(3, expectedVersion);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to confirm topic actuality", e);
        }
    }

    /**
     * Woła NoteService#save po udanym zapisie treści — jeden atomowy UPDATE łączący ewentualne
     * przejście statusu NEW -> NOTE_ADDED (przekazany `status` to już wyliczona wartość docelowa,
     * patrz wywołujący) Z potwierdzeniem aktualności, żeby udany zapis notatki bumpował
     * topic.version DOKŁADNIE RAZ, nie dwa razy pod rząd. Optimistic locking jak wyżej.
     */
    public int markNoteSaved(Connection con, UUID topicId, TopicStatus status, int expectedVersion, Instant verifiedAt) {
        String sql = """
                UPDATE topic SET status = ?, actuality_verified = TRUE, last_verification_of_actuality_date = ?, version = version + 1
                WHERE id = ? AND version = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.from(verifiedAt));
            ps.setObject(3, topicId);
            ps.setInt(4, expectedVersion);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark topic as note-saved", e);
        }
    }

    /**
     * Scheduler (ActualityVerificationScheduler) — jeden zbiorczy, warunkowy UPDATE zamiast
     * wczytywania wszystkich tematów do Javy i filtrowania w pamięci. `cutoff` jest już
     * wyliczonym w Javie (kalendarzowo, zob. ActualityVerificationService) momentem w czasie —
     * rekord traci aktualność, gdy jego data bazowa (ostatnia weryfikacja, a w jej braku
     * createdAt) jest nie później niż `cutoff`. Aktualizuje WYŁĄCZNIE rekordy wciąż oznaczone
     * jako aktualne (actuality_verified = TRUE) — już nieaktualne nigdy nie są dotykane
     * ponownie. version rośnie atomowo w tym samym UPDATE — spójność optimistic lockingu z
     * resztą aplikacji, zob. komentarz w Topic#actualityVerified. RETURNING zwraca pełne wiersze
     * (bez dodatkowego SELECT-a) — potrzebne do rozgłoszenia zdarzeń WebSocket per temat.
     */
    public List<Topic> markStaleAsUnverified(Instant cutoff) {
        String sql = """
                UPDATE topic SET actuality_verified = FALSE, version = version + 1
                WHERE actuality_verified = TRUE AND COALESCE(last_verification_of_actuality_date, created_at) <= ?
                RETURNING *
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark stale topics as unverified", e);
        }
    }

    /**
     * Lista tematów oczekujących na sprawdzenie (GET /api/topics/actuality-review) — wyłącznie
     * actuality_verified = FALSE, opcjonalnie zawężone po autorze, posortowane od najstarszej
     * daty bazowej (COALESCE(last_verification_of_actuality_date, created_at) ASC). Wspierane
     * częściowym indeksem idx_topic_actuality_unverified (user_id, author) WHERE actuality_verified
     * = FALSE — zob. Schema. Nie czyta .kfdoc — wyłącznie kolumny tabeli topic.
     */
    public List<Topic> findForActualityReview(Long userId, String author) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM topic WHERE user_id = ? AND actuality_verified = FALSE");
        if (author != null) sql.append(" AND author = ?");
        sql.append(" ORDER BY COALESCE(last_verification_of_actuality_date, created_at) ASC");

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setLong(1, userId);
            if (author != null) ps.setString(2, author);
            try (ResultSet rs = ps.executeQuery()) {
                List<Topic> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list topics for actuality review", e);
        }
    }
}
