package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.json.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pula pytań quizu (tabela quiz_question). Odpowiedzi trzymane jako JSONB z tablicą 4 tekstów.
 * Wiersze znikają kaskadowo razem z tematem (FK ON DELETE CASCADE).
 */
@Repository
public class QuizQuestionRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private static final RowMapper<QuizQuestion> MAPPER = (rs, n) -> new QuizQuestion(
            rs.getObject("id", UUID.class),
            rs.getObject("topic_id", UUID.class),
            QuizDifficulty.valueOf(rs.getString("difficulty")),
            rs.getString("question"),
            rs.getString("code"),
            readOptions(rs.getString("options")),
            rs.getInt("correct_index"),
            rs.getString("explanation"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    public QuizQuestionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAll(Long userId, List<QuizQuestion> questions) {
        jdbc.batchUpdate("""
                INSERT INTO quiz_question
                    (id, user_id, topic_id, difficulty, question, code, options, correct_index, explanation, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?)
                """, questions, 200, (ps, q) -> {
            ps.setObject(1, q.id());
            ps.setLong(2, userId);
            ps.setObject(3, q.topicId());
            ps.setString(4, q.difficulty().name());
            ps.setString(5, q.question());
            ps.setString(6, q.code());
            ps.setString(7, writeOptions(q.options()));
            ps.setInt(8, q.correctIndex());
            ps.setString(9, q.explanation());
            ps.setTimestamp(10, Timestamp.from(q.createdAt()));
        });
    }

    public List<QuizQuestion> findByTopic(UUID topicId, Long userId) {
        return jdbc.query("SELECT * FROM quiz_question WHERE topic_id = ? AND user_id = ? ORDER BY created_at, id",
                MAPPER, topicId, userId);
    }

    /** Losowe pytania z całej puli — każde wywołanie to nowe losowanie. */
    public List<QuizQuestion> drawRandom(UUID topicId, Long userId, int limit) {
        return jdbc.query("SELECT * FROM quiz_question WHERE topic_id = ? AND user_id = ? ORDER BY random() LIMIT ?",
                MAPPER, topicId, userId, limit);
    }

    public Map<QuizDifficulty, Integer> countByDifficulty(UUID topicId, Long userId) {
        Map<QuizDifficulty, Integer> counts = new EnumMap<>(QuizDifficulty.class);
        for (QuizDifficulty d : QuizDifficulty.values()) counts.put(d, 0);
        jdbc.query("SELECT difficulty, COUNT(*) AS n FROM quiz_question WHERE topic_id = ? AND user_id = ? GROUP BY difficulty",
                rs -> {
                    counts.put(QuizDifficulty.valueOf(rs.getString("difficulty")), rs.getInt("n"));
                }, topicId, userId);
        return counts;
    }

    public int deleteByTopic(UUID topicId, Long userId) {
        return jdbc.update("DELETE FROM quiz_question WHERE topic_id = ? AND user_id = ?", topicId, userId);
    }

    static String writeOptions(List<String> options) {
        try {
            return JsonMapper.get().writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize quiz options", e);
        }
    }

    static List<String> readOptions(String json) {
        try {
            return JsonMapper.get().readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted quiz options: " + json, e);
        }
    }
}
