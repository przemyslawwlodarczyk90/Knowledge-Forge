package com.example.knowledgeforge.domain.exception;

/** Przesłany plik przekracza attachments.max-file-size-mb — mapowane na HTTP 413. */
public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
