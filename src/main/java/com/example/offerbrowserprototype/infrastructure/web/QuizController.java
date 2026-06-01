package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.domain.quiz.dto.QuizDto;
import com.example.offerbrowserprototype.domain.quiz.dto.QuizResultDto;
import com.example.offerbrowserprototype.domain.quiz.dto.SubmitAttemptRequest;
import com.example.offerbrowserprototype.infrastructure.facade.QuizFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizFacade quizFacade;

    @GetMapping("/api/topics/{topicId}/quiz")
    public ResponseEntity<QuizDto> getByTopicId(@PathVariable UUID topicId) {
        return ResponseEntity.ok(quizFacade.getByTopicId(topicId));
    }

    @PostMapping("/api/topics/{topicId}/quiz/generate")
    public ResponseEntity<QuizDto> generate(@PathVariable UUID topicId) {
        QuizDto dto = quizFacade.generate(topicId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @PostMapping("/api/quizzes/attempts")
    public ResponseEntity<QuizResultDto> submitAttempt(@Valid @RequestBody SubmitAttemptRequest request) {
        return ResponseEntity.ok(quizFacade.submitAttempt(request));
    }
}
