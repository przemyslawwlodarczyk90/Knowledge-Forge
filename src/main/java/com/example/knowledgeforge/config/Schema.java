package com.example.knowledgeforge.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import java.util.List;
import java.util.logging.Logger;

/**
 * Zastępuje Hibernate ddl-auto=update. Bezpieczne do uruchamiania przy
 * każdym starcie — działa zarówno na świeżej bazie (same CREATE TABLE),
 * jak i na istniejącej (dokłada/usuwa kolumny, jednorazowo przepisuje
 * stare wartości na nowe). Migracje, które CZYTAJĄ wartość kolumny mającej
 * zniknąć (np. "CASE difficulty WHEN ...") nie mogą być samym "IF EXISTS"
 * w treści zapytania — Postgres waliduje odwołanie do kolumny przy
 * parsowaniu, niezależnie od warunku w WHERE. Dlatego takie migracje są
 * osobnymi metodami, chronione sprawdzeniem information_schema w Javie.
 */
public final class Schema {

    private static final Logger log = Logger.getLogger(Schema.class.getName());

    private static final List<String> STATEMENTS = List.of(
            // UWAGA: tabela users bywa współdzielona z innymi aplikacjami w tej samej bazie —
            // migracje tylko DOKŁADAJĄ brakujące kolumny, nigdy niczego z niej nie usuwają.
            """
            CREATE TABLE IF NOT EXISTS users (
                id BIGSERIAL PRIMARY KEY,
                username TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL,
                password TEXT,
                active BOOLEAN NOT NULL DEFAULT FALSE,
                role TEXT NOT NULL DEFAULT 'USER'
            )
            """,
            // Logowanie wróciło (Spring Security + JWT) — hasło (BCrypt) i rola USER/ADMIN.
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS password TEXT",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'USER'",

            // Jak dla confirmation_tokens, starszy backup może odtwarzać users bez tabeli tokenów.
            """
            CREATE TABLE IF NOT EXISTS password_reset_tokens (
                user_id BIGINT PRIMARY KEY,
                token_hash VARCHAR(64) NOT NULL UNIQUE,
                credentials_hash VARCHAR(64) NOT NULL,
                requested_at TIMESTAMPTZ NOT NULL,
                expires_at TIMESTAMPTZ NOT NULL
            )
            """,

            // Tokeny aktywacyjne z maila rejestracyjnego — ten sam kształt, co na masterze.
            """
            CREATE TABLE IF NOT EXISTS confirmation_tokens (
                id BIGSERIAL PRIMARY KEY,
                token VARCHAR(500) NOT NULL UNIQUE,
                created_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                confirmed_at TIMESTAMP,
                user_id BIGINT NOT NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS category_node (
                id UUID PRIMARY KEY,
                user_id BIGINT NOT NULL REFERENCES users(id),
                parent_id UUID REFERENCES category_node(id),
                name TEXT NOT NULL,
                position INT NOT NULL DEFAULT 0,
                root BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,
            // Opisy kategorii/podkategorii uznane za zbędne — sam tytuł wystarczy.
            "ALTER TABLE category_node DROP COLUMN IF EXISTS description",

            """
            CREATE TABLE IF NOT EXISTS topic (
                id UUID PRIMARY KEY,
                user_id BIGINT NOT NULL REFERENCES users(id),
                category_id UUID NOT NULL REFERENCES category_node(id),
                title TEXT NOT NULL,
                short_prompt TEXT,
                author TEXT,
                detail_level TEXT NOT NULL DEFAULT 'MEDIUM',
                type TEXT NOT NULL DEFAULT 'NOTE',
                status TEXT NOT NULL DEFAULT 'NEW',
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,

            "ALTER TABLE topic ADD COLUMN IF NOT EXISTS author TEXT",

            // Optimistic locking — chroni przed cichym nadpisaniem, gdy dwóch użytkowników
            // edytuje ten sam temat (np. poziom szczegółowości) prawie jednocześnie.
            // Zob. TopicRepository#update / TopicRepository#markNoteSaved (WHERE version = ?).
            "ALTER TABLE topic ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1",

            "ALTER TABLE topic ADD COLUMN IF NOT EXISTS type TEXT",
            "UPDATE topic SET type = 'NOTE' WHERE type IS NULL",
            "ALTER TABLE topic ALTER COLUMN type SET NOT NULL",
            "ALTER TABLE topic DROP COLUMN IF EXISTS is_code",

            "ALTER TABLE topic DROP CONSTRAINT IF EXISTS topic_status_check",
            "UPDATE topic SET status = 'NOTE_ADDED' WHERE status IN ('NOTE_GENERATED', 'QUIZ_READY', 'PASSED', 'MASTERED')",

            // Weryfikacja aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) — DEFAULT TRUE oznacza, że
            // WSZYSTKIE istniejące rekordy po migracji startują jako aktualne, z pustą datą
            // potwierdzenia (termin liczy się wtedy od created_at — zob.
            // ActualityVerificationService). Świadomie NIE ustawiamy im sztucznej daty "teraz" —
            // to zniekształciłoby harmonogram (wszystkie stare rekordy przestałyby być aktualne
            // dokładnie tego samego dnia, `okres` od dziś, zamiast rozłożenia w czasie wg
            // faktycznej daty utworzenia).
            "ALTER TABLE topic ADD COLUMN IF NOT EXISTS actuality_verified BOOLEAN NOT NULL DEFAULT TRUE",
            "ALTER TABLE topic ADD COLUMN IF NOT EXISTS last_verification_of_actuality_date TIMESTAMPTZ",
            // Częściowy indeks — wspiera GET /api/topics/actuality-review (filtr user_id [+author]
            // WHERE actuality_verified = FALSE); rekordów już aktualnych (zwykle większość) w ogóle
            // nie indeksuje, więc jest mały i tani w utrzymaniu.
            """
            CREATE INDEX IF NOT EXISTS idx_topic_actuality_unverified
            ON topic (user_id, author)
            WHERE actuality_verified = FALSE
            """,

            """
            CREATE TABLE IF NOT EXISTS note (
                id UUID PRIMARY KEY,
                user_id BIGINT NOT NULL REFERENCES users(id),
                topic_id UUID NOT NULL UNIQUE REFERENCES topic(id),
                content_path TEXT,
                version INT NOT NULL DEFAULT 1,
                created_at TIMESTAMPTZ NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
            """,
            // Treść notatek przeniesiona na dysk (zob. storage.NoteFileStorage) — baza
            // trzyma tylko ścieżkę. PDF w ogóle nie jest przechowywany — generuje się
            // wyłącznie w przeglądarce, na żądanie. Stare kolumny BYTEA nie miały jeszcze
            // realnych danych poza testowymi, więc ich zawartość nie jest migrowana.
            "ALTER TABLE note ADD COLUMN IF NOT EXISTS content_path TEXT",
            "ALTER TABLE note DROP COLUMN IF EXISTS pdf_path",
            "ALTER TABLE note DROP COLUMN IF EXISTS content_blob",
            "ALTER TABLE note DROP COLUMN IF EXISTS pdf_blob",
            "ALTER TABLE note DROP COLUMN IF EXISTS content_json",
            "ALTER TABLE note DROP COLUMN IF EXISTS prompt_version",
            "ALTER TABLE note DROP COLUMN IF EXISTS ai_model",
            "ALTER TABLE note DROP COLUMN IF EXISTS generated_at",

            // Sprzątanie osieroconych notatek po skasowanym temacie (bez FK w starej bazie)
            "DELETE FROM note WHERE topic_id NOT IN (SELECT id FROM topic)",

            // Dawny, nieużywany model quizu (bez UI i danych) — zastąpiony tabelą quiz_question niżej.
            "DROP TABLE IF EXISTS quiz_attempt",
            "DROP TABLE IF EXISTS user_progress",
            "DROP TABLE IF EXISTS quiz",

            // ── Załączniki — baza trzyma WYŁĄCZNIE metadane + względną ścieżkę (relative_path);
            // same bajty pliku leżą na dysku pod attachments.storage.path (zob. storage.AttachmentStorage).
            // ON DELETE CASCADE to siatka bezpieczeństwa na poziomie integralności danych (żaden
            // rekord attachment nie może przeżyć skasowanego tematu) — ale to NIE zwalnia z usuwania
            // fizycznego pliku w kontrolowany sposób: TopicService.delete() woła najpierw
            // AttachmentService, żeby skasować pliki z dysku (z logowaniem błędów), zanim skasuje
            // sam temat. Diagnostyka (AttachmentDiagnosticsService) wyłapie każdy wyjątek od tej reguły.
            """
            CREATE TABLE IF NOT EXISTS attachment (
                id BIGSERIAL PRIMARY KEY,
                topic_id UUID NOT NULL REFERENCES topic(id) ON DELETE CASCADE,
                original_name TEXT NOT NULL,
                stored_name TEXT NOT NULL,
                relative_path TEXT NOT NULL,
                content_type TEXT,
                size_bytes BIGINT NOT NULL,
                attachment_type TEXT NOT NULL,
                description TEXT,
                checksum_sha256 TEXT NOT NULL,
                created_at TIMESTAMPTZ NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_attachment_topic_id ON attachment(topic_id)",

            // Dzienny licznik generowań AI per użytkownik (zob. ai.AiUsageGuard) — ten sam kształt,
            // co w poprzedniej wersji projektu; UNIQUE (user_id, usage_date) wymagane przez upsert.
            """
            CREATE TABLE IF NOT EXISTS ai_usage (
                id UUID PRIMARY KEY,
                user_id BIGINT NOT NULL,
                usage_date DATE NOT NULL,
                note_generations INT NOT NULL DEFAULT 0,
                quiz_generations INT NOT NULL DEFAULT 0,
                UNIQUE (user_id, usage_date)
            )
            """,

            // Pula pytań quizu z AI (zob. quiz.QuizService) — nowy kształt, celowo pod inną nazwą
            // niż dawne, usuwane wyżej tabele quiz/quiz_attempt. Odpowiedzi: JSONB z 4 tekstami.
            // ON DELETE CASCADE: pytania znikają razem z tematem, bez zmian w TopicService.delete().
            """
            CREATE TABLE IF NOT EXISTS quiz_question (
                id UUID PRIMARY KEY,
                user_id BIGINT NOT NULL REFERENCES users(id),
                topic_id UUID NOT NULL REFERENCES topic(id) ON DELETE CASCADE,
                difficulty TEXT NOT NULL,
                question TEXT NOT NULL,
                code TEXT,
                options JSONB NOT NULL,
                correct_index INT NOT NULL CHECK (correct_index BETWEEN 0 AND 3),
                explanation TEXT NOT NULL,
                created_at TIMESTAMPTZ NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_quiz_question_topic ON quiz_question(topic_id, user_id)"
    );

    private Schema() {
    }

    public static void ensure(DataSource dataSource) {
        try (Connection con = dataSource.getConnection()) {
            migrateDifficultyToDetailLevel(con);
            try (Statement stmt = con.createStatement()) {
                for (String sql : STATEMENTS) {
                    log.fine(() -> "Schema: executing -> " + sql.strip().replaceAll("\\s+", " "));
                    stmt.executeUpdate(sql);
                }
            }
            log.info("Schema check complete — tables ready.");
        } catch (SQLException e) {
            log.severe("Schema migration failed: " + e.getMessage());
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    /**
     * Jednorazowa migracja z poprzedniej wersji: topic.difficulty (BASIC/MEDIUM/HARD,
     * "trudność treści") -> topic.detail_level (LOW/MEDIUM/HIGH, "poziom szczegółowości").
     * Nie-op na świeżej bazie i na każdym kolejnym starcie po pierwszej migracji.
     */
    private static void migrateDifficultyToDetailLevel(Connection con) throws SQLException {
        if (!columnExists(con, "topic", "difficulty")) {
            return;
        }
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("ALTER TABLE topic ADD COLUMN IF NOT EXISTS detail_level TEXT");
            stmt.executeUpdate("""
                    UPDATE topic SET detail_level = CASE difficulty
                        WHEN 'BASIC' THEN 'LOW' WHEN 'MEDIUM' THEN 'MEDIUM' WHEN 'HARD' THEN 'HIGH' ELSE 'MEDIUM' END
                    WHERE detail_level IS NULL
                    """);
            stmt.executeUpdate("ALTER TABLE topic ALTER COLUMN detail_level SET NOT NULL");
            stmt.executeUpdate("ALTER TABLE topic DROP CONSTRAINT IF EXISTS topic_difficulty_check");
            stmt.executeUpdate("ALTER TABLE topic DROP COLUMN difficulty");
            log.info("Migrated topic.difficulty -> topic.detail_level");
        }
    }

    private static boolean columnExists(Connection con, String table, String column) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
