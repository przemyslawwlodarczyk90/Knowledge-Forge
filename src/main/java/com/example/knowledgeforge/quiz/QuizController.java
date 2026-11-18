package com.example.knowledgeforge.quiz;

import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.quiz.dto.GenerateQuizRequest;
import com.example.knowledgeforge.quiz.dto.QuizDto;
import com.example.knowledgeforge.quiz.dto.QuizGenerationResultDto;
import com.example.knowledgeforge.quiz.dto.QuizStatsDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Quiz z AI dla notatki tematu: pula pytań, generowanie kolejnych 100 i losowanie podejścia. */
@RestController
@RequestMapping("/api/topics/{id}/quiz")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public QuizStatsDto stats(@PathVariable String id) {
        return service.stats(topicId(id));
    }

    @GetMapping
    public QuizDto draw(@PathVariable String id, @RequestParam(required = false) Integer count) {
        return service.draw(topicId(id), count);
    }

    @PostMapping("/generate")
    public QuizGenerationResultDto generate(@PathVariable String id,
                                            @Valid @RequestBody(required = false) GenerateQuizRequest body) {
        return service.generate(topicId(id), body);
    }

    @DeleteMapping
    public QuizStatsDto clear(@PathVariable String id) {
        return service.clear(topicId(id));
    }

    private static UUID topicId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + id);
        }
    }
}
