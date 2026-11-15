package com.example.knowledgeforge.ws;

import com.example.knowledgeforge.domain.attachment.dto.AttachmentDto;
import com.example.knowledgeforge.domain.category.dto.CategoryDto;
import com.example.knowledgeforge.domain.topic.TopicStatus;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Fasada publikowania zdarzeń domenowych do wszystkich podłączonych przeglądarek. Serwisy wołają
 * to WYŁĄCZNIE po udanym commicie transakcji — nigdy przed, nigdy gdy transakcja się nie powiodła
 * (zob. komentarze // MULTI-THREADING: w TopicService/NoteService/CategoryService/AttachmentService).
 * Nie wysyła zawartości plików ani treści notatki — tylko metadane/identyfikatory.
 *
 * Każde zdarzenie trafia WYŁĄCZNIE do sesji WebSocket właściciela danych (ownerUserId) — dane są
 * per użytkownik, więc inne zalogowane osoby nie mogą zobaczyć nawet metadanych cudzych tematów.
 */
@Component
public class ApplicationEventHub {

    private static final Logger log = Logger.getLogger(ApplicationEventHub.class.getName());

    private final WebSocketConnectionRegistry registry;
    private final ExecutorService broadcastExecutor;

    public ApplicationEventHub(WebSocketConnectionRegistry registry) {
        this.registry = registry;
        // MULTI-THREADING:
        // Pojedynczy dedykowany wątek na wysyłkę WS, oddzielony od wątku żądania HTTP, który
        // wywołał publish(...). Dzięki temu odpowiedź HTTP (i sam wątek obsługujący żądanie)
        // nie czeka na dosłanie wiadomości do N klientów — serializacja JSON-a i sam broadcast
        // (I/O sieciowe) dzieją się już PO zwolnieniu połączenia z bazą i PO wysłaniu odpowiedzi
        // klientowi, który wykonał operację. Kolejka executor'a jest nieograniczona z rozmysłem:
        // zdarzenia domenowe są małe i nieliczne (nie ma tu ryzyka zalania), a kolejność
        // publikacji per-zasób nie musi być globalnie ścisła — liczy się tylko to, że każde
        // zdarzenie leci już po commicie swojej transakcji.
        this.broadcastExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ws-broadcast");
            t.setDaemon(true);
            return t;
        });
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        broadcastExecutor.shutdown();
        try {
            broadcastExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Tematy ──────────────────────────────────────────────────────

    public void topicCreated(Long ownerUserId, TopicDto topic, int categoryTopicCount, String actorId, String clientId) {
        publish(ownerUserId, "TOPIC_CREATED", topic.getId().toString(), str(topic.getCategoryId()), actorId, clientId,
                Map.of("topic", topic, "currentTopicCount", categoryTopicCount));
    }

    public void topicUpdated(Long ownerUserId, TopicDto topic, String actorId, String clientId) {
        publish(ownerUserId, "TOPIC_UPDATED", topic.getId().toString(), str(topic.getCategoryId()), actorId, clientId,
                Map.of("topic", topic));
    }

    public void topicDeleted(Long ownerUserId, UUID topicId, UUID categoryId, int categoryTopicCount, String actorId, String clientId) {
        publish(ownerUserId, "TOPIC_DELETED", topicId.toString(), str(categoryId), actorId, clientId,
                Map.of("topicId", topicId, "currentTopicCount", categoryTopicCount));
    }

    // ── Notatki — bez treści, tylko status/wersja ────────────────────

    public void noteSaved(Long ownerUserId, UUID noteId, UUID topicId, int version, TopicStatus topicStatus, String actorId, String clientId) {
        publish(ownerUserId, "NOTE_SAVED", noteId.toString(), null, actorId, clientId,
                Map.of("noteId", noteId, "topicId", topicId, "version", version, "topicStatus", topicStatus.name()));
    }

    // ── Załączniki — bez zawartości pliku ────────────────────────────

    public void attachmentCreated(Long ownerUserId, AttachmentDto attachment, String actorId, String clientId) {
        publish(ownerUserId, "ATTACHMENT_CREATED", String.valueOf(attachment.id()), null, actorId, clientId,
                Map.of("attachment", attachment, "topicId", attachment.topicId()));
    }

    public void attachmentDeleted(Long ownerUserId, Long attachmentId, UUID topicId, String actorId, String clientId) {
        publish(ownerUserId, "ATTACHMENT_DELETED", String.valueOf(attachmentId), null, actorId, clientId,
                Map.of("attachmentId", attachmentId, "topicId", topicId));
    }

    // ── Weryfikacja aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) ────────────────────────────
    // Zmiany pojedynczego tematu (ręczne potwierdzenie, zapis notatki) rozgłaszamy przez
    // ZWYKŁE topicUpdated (frontend już umie na to reagować — świeży `version` trafia do
    // otwartych kart, zob. NotePanel). To zdarzenie jest CELOWO osobne i lżejsze — sygnalizuje
    // WYŁĄCZNIE "coś w zbiorze tematów do weryfikacji się zmieniło", żeby otwarty panel
    // "Weryfikacja aktualności" wiedział, że ma się odświeżyć, bez dociągania treści notatek.
    public void actualityReviewChanged(Long ownerUserId, int changedCount, java.util.List<UUID> topicIds) {
        publish(ownerUserId, "ACTUALITY_REVIEW_CHANGED", null, null, null, null,
                Map.of("changedCount", changedCount, "topicIds", topicIds));
    }

    // ── Kategorie ─────────────────────────────────────────────────────

    public void categoryCreated(Long ownerUserId, CategoryDto category, String actorId, String clientId) {
        publish(ownerUserId, "CATEGORY_CREATED", category.getId().toString(), str(category.getParentId()), actorId, clientId,
                Map.of("category", category));
    }

    public void categoryUpdated(Long ownerUserId, CategoryDto category, String actorId, String clientId) {
        publish(ownerUserId, "CATEGORY_UPDATED", category.getId().toString(), str(category.getParentId()), actorId, clientId,
                Map.of("category", category));
    }

    public void categoryDeleted(Long ownerUserId, UUID categoryId, UUID parentId, String actorId, String clientId) {
        publish(ownerUserId, "CATEGORY_DELETED", categoryId.toString(), str(parentId), actorId, clientId,
                Map.of("categoryId", categoryId));
    }

    // ── Wspólne ─────────────────────────────────────────────────────

    private void publish(Long ownerUserId, String type, String entityId, String categoryId, String actorId, String clientId, Object payload) {
        WsEvent event = new WsEvent(UUID.randomUUID().toString(), type, entityId, categoryId, actorId, clientId,
                Instant.now(), payload);
        String json;
        try {
            json = JsonMapper.get().writeValueAsString(event);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to serialize WS event " + type + " for entity " + entityId, e);
            return;
        }
        broadcastExecutor.submit(() -> {
            try {
                registry.broadcast(json, ownerUserId);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unexpected failure broadcasting WS event " + type, e);
            }
        });
    }

    private static String str(Object id) {
        return id == null ? null : id.toString();
    }
}
