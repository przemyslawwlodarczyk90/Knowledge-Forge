package com.example.knowledgeforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NoteFileStorage#writeContent — zapis MUSI być atomowy (plik tymczasowy o unikalnej nazwie w tym
 * samym katalogu + ATOMIC_MOVE), żeby codzienny NoteBundleBackupService, skanujący ten sam katalog
 * bezpośrednio z dysku, nigdy nie trafił na połowicznie zapisany plik. Zob. NoteBundleBackupService.
 */
class NoteFileStorageTest {

    @Test
    void writeContent_creates_readable_file_with_exact_bytes(@TempDir Path tmp) {
        NoteFileStorage storage = new NoteFileStorage(tmp);
        UUID topicId = UUID.randomUUID();
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

        String path = storage.writeContent(topicId, data);

        assertArrayEquals(data, storage.read(path));
    }

    @Test
    void writeContent_overwrite_replaces_content_and_leaves_no_temp_files_behind(@TempDir Path tmp) throws IOException {
        NoteFileStorage storage = new NoteFileStorage(tmp);
        UUID topicId = UUID.randomUUID();

        storage.writeContent(topicId, "version one".getBytes(StandardCharsets.UTF_8));
        String path = storage.writeContent(topicId, "version two".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("version two".getBytes(StandardCharsets.UTF_8), storage.read(path));

        List<Path> filesInDir = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmp)) {
            for (Path p : stream) filesInDir.add(p);
        }
        assertEquals(1, filesInDir.size(), "only the final <topicId>.kfdoc must remain — no leftover .tmp files");
        assertTrue(filesInDir.get(0).getFileName().toString().endsWith(".kfdoc"));
    }

    @Test
    void two_topics_writing_concurrently_do_not_collide_on_the_same_temp_file(@TempDir Path tmp) throws Exception {
        NoteFileStorage storage = new NoteFileStorage(tmp);
        UUID topicA = UUID.randomUUID();
        UUID topicB = UUID.randomUUID();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 20; i++) storage.writeContent(topicA, ("A" + i).getBytes(StandardCharsets.UTF_8));
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 20; i++) storage.writeContent(topicB, ("B" + i).getBytes(StandardCharsets.UTF_8));
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertArrayEquals("A19".getBytes(StandardCharsets.UTF_8), storage.read(tmp.resolve(topicA + ".kfdoc").toString()));
        assertArrayEquals("B19".getBytes(StandardCharsets.UTF_8), storage.read(tmp.resolve(topicB + ".kfdoc").toString()));
    }
}
