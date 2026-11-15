package com.example.knowledgeforge.domain.exception;

/** Przekroczony dzienny limit generowania notatek przez AI (-> 429). */
public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
