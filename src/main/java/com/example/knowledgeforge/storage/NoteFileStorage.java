package com.example.knowledgeforge.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
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

    /**
     * Zapisuje treść notatki (.kfdoc) na dysk ATOMOWO i zwraca jej ścieżkę (do zapisania w bazie).
     * Zapis idzie najpierw do unikalnej nazwy tymczasowej w TYM SAMYM katalogu (UUID losowany na
     * nowo za każdym wywołaniem — celowo NIE jeden stały plik ".part", żeby dwa równoległe zapisy
     * różnych tematów nigdy nie kolidowały o ten sam plik tymczasowy), a dopiero po pełnym
     * zapisaniu i zamknięciu strumienia — ATOMIC_MOVE (z bezpiecznym fallbackiem REPLACE_EXISTING,
     * gdy system plików nie wspiera atomowego przenoszenia) podmienia docelowy plik. Dzięki temu
     * codzienny NoteBundleBackupService (zob. backup/NoteBundleBackupService.java), skanujący ten
     * sam katalog bezpośrednio z dysku, zawsze trafia albo na POPRZEDNIĄ, albo na NOWĄ kompletną
     * wersję pliku — nigdy na połowicznie zapisaną zawartość.
     */
    public String writeContent(UUID topicId, byte[] data) {
        Path path = contentPath(topicId);
        Path tmp = baseDir.resolve(topicId + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(tmp, data);
            moveAtomically(tmp, path);
            log.fine(() -> "Wrote " + data.length + " bytes -> " + path);
            return path.toString();
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new UncheckedIOException("Failed to write note file: " + path, e);
        }
    }

    private static void moveAtomically(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicNotSupported) {
            // Niektóre systemy plików (np. pewne sieciowe udziały) nie wspierają ATOMIC_MOVE —
            // bezpieczny fallback z samym REPLACE_EXISTING (wciąż jedna operacja rename, nie
            // strumieniowy zapis do pliku docelowego).
            Files.move(from, to, (CopyOption) StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.log(Level.WARNING, e, () -> "Failed to remove leftover temp file: " + path);
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
