package com.example.knowledgeforge.domain.exception;

/**
 * Zastępuje adnotacje jakarta.validation (@NotBlank, @NotNull, ...) —
 * bez frameworka walidacja jest ręczna, w serwisach; ten wyjątek
 * mapuje się na HTTP 400, tak jak wcześniej MethodArgumentNotValidException.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
