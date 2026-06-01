package com.example.offerbrowserprototype.domain.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ai_usage",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "usage_date"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "note_generations", nullable = false)
    @Builder.Default
    private Integer noteGenerations = 0;

    @Column(name = "quiz_generations", nullable = false)
    @Builder.Default
    private Integer quizGenerations = 0;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.noteGenerations == null) {
            this.noteGenerations = 0;
        }
        if (this.quizGenerations == null) {
            this.quizGenerations = 0;
        }
    }
}
