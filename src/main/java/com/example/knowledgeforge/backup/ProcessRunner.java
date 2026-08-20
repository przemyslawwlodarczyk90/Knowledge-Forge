package com.example.knowledgeforge.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Uruchamia pg_dump/pg_restore przez ProcessBuilder (bez budowania komendy jako jednego
 * Stringa — argumenty trafiają do procesu jako lista, bez interpretacji przez powłokę).
 * Drenuje stderr w OSOBNYM wątku, w trakcie działania procesu — czytanie stderr dopiero
 * PO process.waitFor() mogłoby się zawiesić, gdyby narzędzie zapisało więcej niż mieści
 * bufor potoku systemowego (zwykle ok. 64KB) i zablokowało się na pełnym pipe.
 */
public final class ProcessRunner {

    private static final int MAX_STDERR_CAPTURE = 65536;

    private ProcessRunner() {
    }

    public record Outcome(int exitCode, String stderr) {
    }

    public static Outcome run(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();
        process.getOutputStream().close(); // pg_dump/pg_restore nie czytają stdin

        StringBuilder captured = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (InputStream es = process.getErrorStream()) {
                byte[] buf = new byte[4096];
                int total = 0;
                int read;
                while ((read = es.read(buf)) != -1) {
                    if (total < MAX_STDERR_CAPTURE) {
                        int toAppend = Math.min(read, MAX_STDERR_CAPTURE - total);
                        synchronized (captured) {
                            captured.append(new String(buf, 0, toAppend, StandardCharsets.UTF_8));
                        }
                        total += toAppend;
                    }
                    // Powyżej limitu dalej drenujemy (żeby proces się nie zablokował), po prostu nie zapamiętujemy więcej.
                }
            } catch (IOException ignored) {
                // strumień zamknięty razem z zakończeniem procesu — nic do zrobienia
            }
        }, "pg-process-stderr-reader");
        reader.setDaemon(true);
        reader.start();

        int exitCode = process.waitFor();
        reader.join(5000);
        String stderr;
        synchronized (captured) {
            stderr = captured.toString();
        }
        return new Outcome(exitCode, stderr);
    }

    /** Usuwa hasło z tekstu przed zalogowaniem — hasło samo w sobie nigdy nie trafia do argumentów/logów, to tylko dodatkowa siatka. */
    public static String scrub(String text, String password) {
        if (text == null) return "";
        if (password != null && !password.isEmpty()) {
            return text.replace(password, "***");
        }
        return text;
    }
}
