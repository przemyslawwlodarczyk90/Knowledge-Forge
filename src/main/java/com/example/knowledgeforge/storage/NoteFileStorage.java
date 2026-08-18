package com.example.knowledgeforge.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Baza danych trzyma tylko ścieżkę (String) do pliku treści — sama notatka
 * (.kfdoc) leży jako zwykły plik na dysku, pod {@code notes.storage.path}.
 * Aplikacja wczytuje ją z dysku na żądanie; podczas edycji treść żyje w
 * pamięci przeglądarki (edytor), tak jak w każdym edytorze tekstu.
 * PDF nie jest tu w ogóle trzymany — generuje się wyłącznie w przeglądarce,
 * na żądanie, żeby setki notatek nie puchły plikami, których nikt nie pobierze.
 */
public class NoteFileStorage {

    private static final Logger log = Logger.getLogger(NoteFileStorage.class.getName());

    private final Path baseDir;

    public NoteFileStorage(Path baseDir) {
        this.baseDir = baseDir;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create notes storage folder: " + baseDir, e);
        }
        log.info(() -> "Notes storage folder: " + baseDir.toAbsolutePath());
    }

    public Path baseDir() {
        return baseDir;
    }

    /** Zapisuje treść notatki (.kfdoc) na dysk i zwraca jej ścieżkę (do zapisania w bazie). */
    public String writeContent(UUID topicId, byte[] data) {
        Path path = contentPath(topicId);
        try {
            Files.write(path, data);
            log.fine(() -> "Wrote " + data.length + " bytes -> " + path);
            return path.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write note file: " + path, e);
        }
    }

    public byte[] read(String path) {
        if (path == null) return null;
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            log.warning(() -> "Failed to read note file: " + path + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private Path contentPath(UUID topicId) {
        return baseDir.resolve(topicId + ".kfdoc");
    }
}
