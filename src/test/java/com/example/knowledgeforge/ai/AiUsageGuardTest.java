package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.DailyLimitExceededException;
import com.example.knowledgeforge.support.TestJpa;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Integracyjny (kf_test) — atomowy upsert licznika, także pod współbieżnością. */
class AiUsageGuardTest {

    static AiUsageGuard guard(int limit, Clock clock) {
        Properties p = new Properties();
        p.setProperty("ai.daily-note-limit", String.valueOf(limit));
        p.setProperty("ai.daily-quiz-limit", String.valueOf(limit));
        return new AiUsageGuard(TestJpa.get(JdbcTemplate.class), AppConfig.fromProperties(p), clock);
    }

    static long randomUserId() {
        return 1_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L);
    }

    @Test
    void counts_up_to_limit_then_rejects_and_resets_next_day() {
        long user = randomUserId();
        Clock day1 = Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"), ZoneOffset.UTC);
        AiUsageGuard g = guard(2, day1);
        assertThat(g.checkAndIncrementNote(user)).isEqualTo(1);
        assertThat(g.checkAndIncrementNote(user)).isEqualTo(2);
        assertThatThrownBy(() -> g.checkAndIncrementNote(user)).isInstanceOf(DailyLimitExceededException.class);

        AiUsageGuard nextDay = guard(2, Clock.fixed(Instant.parse("2030-01-02T10:00:00Z"), ZoneOffset.UTC));
        assertThat(nextDay.checkAndIncrementNote(user)).isEqualTo(1);
    }

    @Test
    void concurrent_requests_never_exceed_the_limit() throws Exception {
        long user = randomUserId();
        AiUsageGuard g = guard(5, Clock.systemUTC());
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger ok = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            fs.add(pool.submit(() -> {
                try {
                    g.checkAndIncrementNote(user);
                    ok.incrementAndGet();
                } catch (DailyLimitExceededException ignored) {
                }
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        assertThat(ok.get()).isEqualTo(5);
    }

    @Test
    void quiz_counter_is_separate_from_note_counter() {
        long user = randomUserId();
        Clock day = Clock.fixed(Instant.parse("2030-02-01T10:00:00Z"), ZoneOffset.UTC);
        AiUsageGuard g = guard(1, day);
        assertThat(g.checkAndIncrementNote(user)).isEqualTo(1);
        assertThat(g.checkAndIncrementQuiz(user)).isEqualTo(1);
        assertThatThrownBy(() -> g.checkAndIncrementQuiz(user))
                .isInstanceOf(DailyLimitExceededException.class).hasMessageContaining("quiz");
        assertThatThrownBy(() -> g.checkAndIncrementNote(user)).isInstanceOf(DailyLimitExceededException.class);
    }
}
