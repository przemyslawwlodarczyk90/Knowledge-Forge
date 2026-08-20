package com.example.knowledgeforge.storage;

import com.example.knowledgeforge.domain.exception.PayloadTooLargeException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentStorageTest {

    @Test
    void save_streams_file_computes_checksum_and_never_uses_original_name_on_disk(@TempDir Path tempDir) throws Exception {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        byte[] content = "hello world attachment content".getBytes(StandardCharsets.UTF_8);

        AttachmentStorage.SaveResult result = storage.save(new ByteArrayInputStream(content), "report.PDF", 1024);

        assertTrue(result.storedName().endsWith(".pdf"), "extension is preserved (lowercased)");
        assertNotEquals("report.PDF", result.storedName(), "physical name is never the original name");
        assertEquals(content.length, result.sizeBytes());

        String expectedSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        assertEquals(expectedSha, result.checksumSha256());

        Path onDisk = storage.resolveExisting(result.relativePath());
        assertArrayEquals(content, Files.readAllBytes(onDisk));

        // yyyy/MM/<uuid>.ext, zawsze z "/" niezależnie od OS
        assertTrue(result.relativePath().matches("\\d{4}/\\d{2}/[0-9a-fA-F-]+\\.pdf"), result.relativePath());
    }

    @Test
    void save_ignores_path_traversal_attempts_in_original_name(@TempDir Path tempDir) throws Exception {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);

        AttachmentStorage.SaveResult result = storage.save(
                new ByteArrayInputStream(content), "../../../etc/passwd.txt", 1024);

        assertTrue(result.storedName().endsWith(".txt"));
        assertFalse(result.relativePath().contains(".."));
        Path resolved = storage.resolveExisting(result.relativePath());
        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void resolveExisting_blocks_traversal_outside_base_dir(@TempDir Path tempDir) {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        assertThrows(ValidationException.class, () -> storage.resolveExisting("../outside.txt"));
        assertThrows(ValidationException.class, () -> storage.resolveExisting("2026/../../outside.txt"));
    }

    @Test
    void save_rejects_files_over_the_configured_limit(@TempDir Path tempDir) {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        byte[] content = new byte[2000];
        assertThrows(PayloadTooLargeException.class, () ->
                storage.save(new ByteArrayInputStream(content), "big.bin", 1000));
    }

    @Test
    void save_rejects_empty_file(@TempDir Path tempDir) {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        assertThrows(ValidationException.class, () ->
                storage.save(new ByteArrayInputStream(new byte[0]), "empty.txt", 1024));
    }

    @Test
    void no_leftover_temp_files_after_size_limit_failure(@TempDir Path tempDir) throws IOException {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        byte[] content = new byte[2000];
        assertThrows(PayloadTooLargeException.class, () ->
                storage.save(new ByteArrayInputStream(content), "big.bin", 1000));
        try (var walk = Files.walk(tempDir)) {
            assertTrue(walk.filter(Files::isRegularFile).findAny().isEmpty(), "no .part or partial file left behind");
        }
    }

    @Test
    void delete_returns_false_for_missing_file_without_throwing(@TempDir Path tempDir) {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        assertFalse(storage.delete("2026/08/does-not-exist.bin"));
    }

    @Test
    void save_then_delete_removes_file_from_disk(@TempDir Path tempDir) throws Exception {
        AttachmentStorage storage = new AttachmentStorage(tempDir);
        AttachmentStorage.SaveResult result = storage.save(
                new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), "note.txt", 1024);

        assertTrue(storage.exists(result.relativePath()));
        assertTrue(storage.delete(result.relativePath()));
        assertFalse(storage.exists(result.relativePath()));
    }
}
