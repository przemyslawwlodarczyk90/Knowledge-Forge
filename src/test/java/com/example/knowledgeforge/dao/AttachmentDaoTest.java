package com.example.knowledgeforge.dao;

import com.example.knowledgeforge.domain.attachment.Attachment;
import com.example.knowledgeforge.domain.attachment.AttachmentType;
import com.example.knowledgeforge.support.TestDatabase;
import com.example.knowledgeforge.support.TestFixtures;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Integracyjny — łączy się z osobną bazą testową kf_test (zob. TestDatabase), nigdy z bazą roboczą. */
class AttachmentDaoTest {

    static HikariDataSource ds;
    static long userId;
    static UUID categoryId;
    static UUID topicId;

    AttachmentDao dao;

    @BeforeAll
    static void setUpAll() throws Exception {
        ds = TestDatabase.create();
        userId = TestFixtures.insertUser(ds, "att-test-" + UUID.randomUUID());
        categoryId = TestFixtures.insertCategory(ds, userId, "Test category");
        topicId = TestFixtures.insertTopic(ds, userId, categoryId, "Test topic");
    }

    @AfterAll
    static void tearDownAll() {
        if (ds != null) ds.close();
    }

    @BeforeEach
    void setUp() {
        dao = new AttachmentDao(ds);
    }

    @Test
    void insert_then_findById_roundtrips_metadata_and_topic_relation() {
        Attachment a = sample("report.xlsx", AttachmentType.FILE, 12345L);
        dao.insert(a);
        assertNotNull(a.getId());

        Attachment found = dao.findById(a.getId()).orElseThrow();
        assertEquals(topicId, found.getTopicId());
        assertEquals("report.xlsx", found.getOriginalName());
        assertEquals(AttachmentType.FILE, found.getAttachmentType());
        assertEquals(12345L, found.getSizeBytes());
        assertEquals("deadbeef", found.getChecksumSha256());
        assertEquals("test description", found.getDescription());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    void insert_video_classification_persists_correctly() {
        Attachment a = sample("teams-recording.mp4", AttachmentType.VIDEO, 999_000L);
        dao.insert(a);

        Attachment found = dao.findById(a.getId()).orElseThrow();
        assertEquals(AttachmentType.VIDEO, found.getAttachmentType());
    }

    @Test
    void findAllByTopicId_returns_only_that_topics_attachments_newest_first() {
        Attachment a1 = sample("first.txt", AttachmentType.FILE, 10L);
        dao.insert(a1);
        Attachment a2 = sample("second.txt", AttachmentType.FILE, 20L);
        dao.insert(a2);

        List<Attachment> list = dao.findAllByTopicId(topicId);
        assertTrue(list.size() >= 2);
        assertTrue(list.stream().allMatch(a -> a.getTopicId().equals(topicId)));
        // insert() ustawia created_at w konstruktorze DAO -> a2 powinien być >= a1 w kolejności DESC
        int idx1 = indexOfId(list, a1.getId());
        int idx2 = indexOfId(list, a2.getId());
        assertTrue(idx2 <= idx1, "newer attachment should not sort after an older one (ORDER BY created_at DESC)");
    }

    @Test
    void delete_removes_the_record() {
        Attachment a = sample("to-delete.bin", AttachmentType.FILE, 1L);
        dao.insert(a);
        Long id = a.getId();

        dao.delete(id);

        assertTrue(dao.findById(id).isEmpty());
    }

    @Test
    void findAllRelativePaths_includes_inserted_paths() {
        Attachment a = sample("for-diagnostics.bin", AttachmentType.FILE, 1L);
        dao.insert(a);

        assertTrue(dao.findAllRelativePaths().contains(a.getRelativePath()));
    }

    @Test
    void attachment_cascade_deletes_when_topic_is_deleted() throws Exception {
        UUID cascadeTopicId = TestFixtures.insertTopic(ds, userId, categoryId, "Cascade test topic");
        Attachment a = new Attachment();
        a.setTopicId(cascadeTopicId);
        a.setOriginalName("cascade.txt");
        a.setStoredName(UUID.randomUUID() + ".txt");
        a.setRelativePath("2026/08/" + a.getStoredName());
        a.setContentType("text/plain");
        a.setSizeBytes(5L);
        a.setAttachmentType(AttachmentType.FILE);
        a.setChecksumSha256("cafebabe");
        dao.insert(a);

        assertTrue(dao.findById(a.getId()).isPresent());
        TestFixtures.deleteTopic(ds, cascadeTopicId);
        assertFalse(TestFixtures.topicExists(ds, cascadeTopicId));

        // ON DELETE CASCADE — rekord attachment znika razem z tematem na poziomie integralności bazy
        // (kontrolowane kasowanie PLIKU z dysku to osobna odpowiedzialność AttachmentService, nie DAO/schema).
        assertTrue(dao.findById(a.getId()).isEmpty());
    }

    private static int indexOfId(List<Attachment> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private Attachment sample(String originalName, AttachmentType type, long size) {
        Attachment a = new Attachment();
        a.setTopicId(topicId);
        a.setOriginalName(originalName);
        a.setStoredName(UUID.randomUUID() + "-" + originalName);
        a.setRelativePath("2026/08/" + a.getStoredName());
        a.setContentType("application/octet-stream");
        a.setSizeBytes(size);
        a.setAttachmentType(type);
        a.setDescription("test description");
        a.setChecksumSha256("deadbeef");
        return a;
    }
}
