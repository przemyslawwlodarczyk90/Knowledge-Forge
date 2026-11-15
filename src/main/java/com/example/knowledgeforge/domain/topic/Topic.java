package com.example.knowledgeforge.domain.topic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic")
public class Topic implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "short_prompt")
    private String shortPrompt;

    @Column(name = "author")
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_level", nullable = false)
    private DetailLevel detailLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TopicType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopicStatus status = TopicStatus.NEW;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    /** Optimistic locking — zob. TopicRepository#update / TopicRepository#markNoteSaved. */
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    /**
     * Mechanizm okresowej weryfikacji aktualności (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) —
     * true = rekord uznany za aktualny; false = wymaga sprawdzenia. Nowy temat startuje jako
     * aktualny, bez daty potwierdzenia (rozliczany od createdAt, dopóki ktoś świadomie nie
     * potwierdzi/nie zapisze treści). Zmieniane WYŁĄCZNIE przez: POST .../verify-actuality,
     * udany zapis notatki (NoteService#save) i okresowy ActualityVerificationScheduler —
     * NIGDY przez ogólny PATCH /api/topics/{id}.
     */
    @Column(name = "actuality_verified", nullable = false)
    private boolean actualityVerified = true;
    /** Czas ostatniego ŚWIADOMEGO potwierdzenia/aktualizacji treści — null, dopóki nikt tego nie zrobił. */
    @Column(name = "last_verification_of_actuality_date")
    private Instant lastVerificationOfActualityDate;

    public Topic() {
    }

    /**
     * Id (UUID) nadaje aplikacja, nie baza — bez tej flagi Spring Data uznałby każdą encję z
     * ustawionym id za istniejącą i zamiast INSERT-u zrobił merge (SELECT + INSERT).
     */
    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    @Override
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
