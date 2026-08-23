package com.example.knowledgeforge.domain.topic;

import java.time.Instant;
import java.util.UUID;

public class Topic {

    private UUID id;
    private Long userId;
    private UUID categoryId;
    private String title;
    private String shortPrompt;
    private String author;
    private DetailLevel detailLevel;
    private TopicType type;
    private TopicStatus status = TopicStatus.NEW;
    private Instant createdAt;
    private Instant updatedAt;
    /** Optimistic locking — zob. TopicDao#update / TopicDao#updateStatus. */
    private Integer version = 1;
    /**
     * Mechanizm okresowej weryfikacji aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) —
     * true = rekord uznany za aktualny; false = wymaga sprawdzenia. Nowy temat startuje jako
     * aktualny, bez daty potwierdzenia (rozliczany od createdAt, dopóki ktoś świadomie nie
     * potwierdzi/nie zapisze treści). Zmieniane WYŁĄCZNIE przez: POST .../verify-actuality,
     * udany zapis notatki (NoteService#save) i okresowy ActualityVerificationScheduler —
     * NIGDY przez ogólny PATCH /api/topics/{id}.
     */
    private boolean actualityVerified = true;
    /** Czas ostatniego ŚWIADOMEGO potwierdzenia/aktualizacji treści — null, dopóki nikt tego nie zrobił. */
    private Instant lastVerificationOfActualityDate;

    public Topic() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getShortPrompt() { return shortPrompt; }
    public void setShortPrompt(String shortPrompt) { this.shortPrompt = shortPrompt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public DetailLevel getDetailLevel() { return detailLevel; }
    public void setDetailLevel(DetailLevel detailLevel) { this.detailLevel = detailLevel; }

    public TopicType getType() { return type; }
    public void setType(TopicType type) { this.type = type; }

    public TopicStatus getStatus() { return status; }
    public void setStatus(TopicStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public boolean isActualityVerified() { return actualityVerified; }
    public void setActualityVerified(boolean actualityVerified) { this.actualityVerified = actualityVerified; }

    public Instant getLastVerificationOfActualityDate() { return lastVerificationOfActualityDate; }
    public void setLastVerificationOfActualityDate(Instant lastVerificationOfActualityDate) { this.lastVerificationOfActualityDate = lastVerificationOfActualityDate; }
}
