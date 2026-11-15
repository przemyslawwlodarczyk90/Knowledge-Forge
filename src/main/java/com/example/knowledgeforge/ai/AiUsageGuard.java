package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.DailyLimitExceededException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Dzienny limit generowania notatek per użytkownik (tabela ai_usage — ta sama co w poprzedniej
 * wersji projektu). Licznik rośnie PRZED wywołaniem modelu, jednym atomowym upsertem warunkowym
 * (INSERT ... ON CONFLICT DO UPDATE ... WHERE note_generations < limit) — dwa równoległe żądania
 * nie przekroczą limitu, a każde wywołanie płatnego API jest policzone.
 */
@Service
public class AiUsageGuard {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final JdbcTemplate jdbc;
    private final AppConfig config;
    private final Clock clock;

    public AiUsageGuard(JdbcTemplate jdbc, AppConfig config, Clock clock) {
        this.jdbc = jdbc;
        this.config = config;
        this.clock = clock;
    }

    /** Zwraca liczbę wykorzystanych dziś generowań (po zwiększeniu) albo rzuca DailyLimitExceededException. */
    public int checkAndIncrementNote(Long userId) {
        int limit = config.aiDailyNoteLimit();
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<Integer> updated = jdbc.queryForList("""
                INSERT INTO ai_usage (id, user_id, usage_date, note_generations, quiz_generations)
                VALUES (?, ?, ?, 1, 0)
                ON CONFLICT (user_id, usage_date)
                DO UPDATE SET note_generations = ai_usage.note_generations + 1
                WHERE ai_usage.note_generations < ?
                RETURNING note_generations
                """, Integer.class, UUID.randomUUID(), userId, today, limit);
        if (updated.isEmpty() || updated.get(0) > limit) {
            throw new DailyLimitExceededException("Daily note generation limit reached (" + limit + ")");
        }
        return updated.get(0);
    }
}
