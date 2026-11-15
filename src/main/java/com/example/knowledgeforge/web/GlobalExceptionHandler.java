package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.exception.AiNotConfiguredException;
import com.example.knowledgeforge.domain.exception.AiResponseException;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.DailyLimitExceededException;
import com.example.knowledgeforge.domain.exception.ForbiddenException;
import com.example.knowledgeforge.domain.exception.NotFoundException;
import com.example.knowledgeforge.domain.exception.PayloadTooLargeException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapowanie wyjątków domenowych na kody HTTP — dokładnie to, co robił dawny ApiServlet#handle:
 * zawsze JSON {"message": "..."}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Map<String, String>> conflict(ConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    ResponseEntity<Map<String, String>> tooLarge(PayloadTooLargeException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> uploadTooLarge(MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the configured size limit");
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<Map<String, String>> badMultipart(MultipartException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed multipart upload: " + e.getMessage());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    ResponseEntity<Map<String, String>> dailyLimit(DailyLimitExceededException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(AiNotConfiguredException.class)
    ResponseEntity<Map<String, String>> aiNotConfigured(AiNotConfiguredException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(AiResponseException.class)
    ResponseEntity<Map<String, String>> aiResponse(AiResponseException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Map<String, String>> forbidden(ForbiddenException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> malformedJson(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed JSON body: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception e, HttpServletRequest req) {
        // Wyjątki samego Spring MVC (brak trasy, zła metoda, brak parametru...) niosą własny status.
        if (e instanceof ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            return error(status, er.getBody().getDetail());
        }
        log.log(Level.SEVERE, "Error handling " + req.getMethod() + " " + req.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message == null ? "" : message));
    }
}
