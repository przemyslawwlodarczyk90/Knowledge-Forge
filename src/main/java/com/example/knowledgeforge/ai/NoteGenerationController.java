package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.ai.dto.GenerateNoteRequest;
import com.example.knowledgeforge.ai.dto.GeneratedNoteDto;
import com.example.knowledgeforge.domain.exception.ValidationException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** POST /api/topics/{id}/note/generate — szkic notatki z OpenAI (nie zapisuje notatki). */
@RestController
public class NoteGenerationController {

    private final NoteGenerationService service;

    public NoteGenerationController(NoteGenerationService service) {
        this.service = service;
    }

    @PostMapping("/api/topics/{id}/note/generate")
    public GeneratedNoteDto generate(@PathVariable String id, @RequestBody(required = false) GenerateNoteRequest body) {
        UUID topicId;
        try {
            topicId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + id);
        }
        return service.generate(topicId, body);
    }
}
