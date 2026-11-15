package com.example.knowledgeforge.domain.exception;

/** Model zwrócił błąd albo odpowiedź niezgodną ze strukturą notatki (-> 502). */
public class AiResponseException extends RuntimeException {
    public AiResponseException(String message) {
        super(message);
    }
}
