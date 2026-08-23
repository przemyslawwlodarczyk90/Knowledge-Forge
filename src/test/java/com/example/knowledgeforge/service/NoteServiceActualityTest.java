package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.note.dto.NoteDto;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.support.ServiceHarness;
import com.example.knowledgeforge.support.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Zapis notatki (NoteService#save) jako świadome potwierdzenie aktualności tematu —
 * zob. dokumentacja/ACTUALITY_VERIFICATION.txt punkt 6. Udany zapis (pierwszy i kolejny) musi ustawić
 * actualityVerified=true + świeżą lastVerificationOfActualityDate; nieudany (409) nie może
 * ruszyć stanu aktualności.
 */
class NoteServiceActualityTest {

    ServiceHarness harness;
    long userId;
    UUID categoryId;
    static final ObjectMapper JSON = new ObjectMapper();

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        harness = ServiceHarness.withFreshUser(tempDir, "note-actuality");
        userId = harness.currentUser.id();
        categoryId = TestFixtures.insertCategory(harness.dataSource, userId, "Note actuality test category");
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    private JsonNode simpleDoc(String text) throws Exception {
        return JSON.readTree("""
                { "type": "doc", "content": [ { "type": "paragraph", "content": [ { "type": "text", "text": "%s" } ] } ] }
                """.formatted(text));
    }

    @Test
    void first_note_save_confirms_actuality() throws Exception {
        TopicDto topic = harness.topicService.create(
                new CreateTopicRequest(categoryId, "First save", null, null, DetailLevel.MEDIUM, TopicType.NOTE), "c1");
        // Marks the topic as pre-existing/stale to prove the save actually FLIPS it, not just
        // leaves an already-true default untouched. Cutoff far in the future -> covers everything.
        harness.topicDao.markStaleAsUnverified(Instant.now().plusSeconds(3600));
        assertFalse(harness.topicService.getById(topic.getId()).isActualityVerified());

        NoteDto saved = harness.noteService.save(topic.getId(),
                new SaveNoteRequest(simpleDoc("hello"), List.of(), null), "c1");
        assertEquals(1, saved.getVersion());

        TopicDto reloaded = harness.topicService.getById(topic.getId());
        assertTrue(reloaded.isActualityVerified());
        assertNotNullRecent(reloaded.getLastVerificationOfActualityDate());
    }

    @Test
    void update_of_existing_note_also_confirms_actuality() throws Exception {
        TopicDto topic = harness.topicService.create(
                new CreateTopicRequest(categoryId, "Update save", null, null, DetailLevel.MEDIUM, TopicType.NOTE), "c1");
        NoteDto first = harness.noteService.save(topic.getId(),
                new SaveNoteRequest(simpleDoc("v1"), List.of(), null), "c1");

        // Force the topic back to "needs review" between the two saves, to prove the SECOND
        // (update) save re-confirms it too, not just the first.
        harness.topicDao.markStaleAsUnverified(Instant.now().plusSeconds(3600));
        assertFalse(harness.topicService.getById(topic.getId()).isActualityVerified());

        NoteDto updated = harness.noteService.save(topic.getId(),
                new SaveNoteRequest(simpleDoc("v2"), List.of(), first.getVersion()), "c1");
        assertEquals(2, updated.getVersion());

        TopicDto reloaded = harness.topicService.getById(topic.getId());
        assertTrue(reloaded.isActualityVerified(), "updating an existing note must also confirm actuality");
        assertNotNullRecent(reloaded.getLastVerificationOfActualityDate());
    }

    @Test
    void failed_note_save_conflict_does_not_confirm_actuality() throws Exception {
        TopicDto topic = harness.topicService.create(
                new CreateTopicRequest(categoryId, "Conflicting save", null, null, DetailLevel.MEDIUM, TopicType.NOTE), "c1");
        NoteDto first = harness.noteService.save(topic.getId(),
                new SaveNoteRequest(simpleDoc("v1"), List.of(), null), "c1");

        harness.topicDao.markStaleAsUnverified(Instant.now().plusSeconds(3600));
        assertFalse(harness.topicService.getById(topic.getId()).isActualityVerified());

        // baseVersion voluntarily wrong -> 409, save must not go through.
        assertThrows(ConflictException.class, () -> harness.noteService.save(topic.getId(),
                new SaveNoteRequest(simpleDoc("v2-conflicting"), List.of(), 999), "c1"));

        TopicDto reloaded = harness.topicService.getById(topic.getId());
        assertFalse(reloaded.isActualityVerified(), "a rejected (409) save must NOT confirm actuality");
    }

    private void assertNotNullRecent(Instant instant) {
        org.junit.jupiter.api.Assertions.assertNotNull(instant);
        assertTrue(instant.isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)), "verification date must be freshly set to ~now");
    }
}
