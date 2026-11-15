package com.example.knowledgeforge.domain.exception;

/** Generowanie AI wyłączone — brak OPENAI_API_KEY (-> 503). */
public class AiNotConfiguredException extends RuntimeException {
    public AiNotConfiguredException(String message) {
        super(message);
    }
}
