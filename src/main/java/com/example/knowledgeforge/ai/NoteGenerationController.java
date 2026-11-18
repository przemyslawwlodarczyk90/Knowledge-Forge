package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.ai.dto.GenerateNoteRequest;
import com.example.knowledgeforge.ai.dto.GeneratedNoteDto;
import com.example.knowledgeforge.ai.dto.RefineNoteRequest;
import com.example.knowledgeforge.domain.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Tworzenie i dopracowywanie szkicu notatki przez OpenAI. */
@RestController
public class NoteGenerationController {

    private final NoteGenerationService service;

    public NoteGenerationController(NoteGenerationService service) {
        this.service = service;
    }

    @PostMapping("/api/topics/{id}/note/generate")
    public GeneratedNoteDto generate(@PathVariable String id, @RequestBody(required = false) GenerateNoteRequest body) {
        return service.generate(topicId(id), body);
    }

    @PostMapping("/api/topics/{id}/note/refine")
    public GeneratedNoteDto refine(@PathVariable String id, @Valid @RequestBody RefineNoteRequest body) {
        return service.refine(topicId(id), body);
    }

    private static UUID topicId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + id);
        }
    }
}
