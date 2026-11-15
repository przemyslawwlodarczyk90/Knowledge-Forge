package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.domain.topic.DetailLevel;

/** Głębokość generowanej notatki (BASIC / MEDIUM / HARD) — pojęcie z promptu, niezależne od DetailLevel tematu. */
public enum NoteDifficulty {
    BASIC, MEDIUM, HARD;

    /** Domyślne odwzorowanie z poziomu szczegółowości tematu, gdy klient nie poda trudności. */
    public static NoteDifficulty fromDetailLevel(DetailLevel level) {
        if (level == null) return MEDIUM;
        return switch (level) {
            case LOW -> BASIC;
            case MEDIUM -> MEDIUM;
            case HIGH -> HARD;
        };
    }
}
