package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.domain.note.NoteContent;
import com.example.offerbrowserprototype.domain.note.dto.NoteDto;
import com.example.offerbrowserprototype.infrastructure.facade.NoteFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/topics/{topicId}/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteFacade noteFacade;

    @GetMapping
    public ResponseEntity<NoteDto> get(@PathVariable UUID topicId) {
        return ResponseEntity.ok(noteFacade.getByTopicId(topicId));
    }

    @PostMapping
    public ResponseEntity<NoteDto> createManual(
            @PathVariable UUID topicId,
            @RequestBody NoteContent content) {
        NoteDto dto = noteFacade.createManual(topicId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping
    public ResponseEntity<NoteDto> updateManual(
            @PathVariable UUID topicId,
            @RequestBody NoteContent content) {
        return ResponseEntity.ok(noteFacade.updateManual(topicId, content));
    }

    @PostMapping("/generate")
    public ResponseEntity<NoteDto> generate(@PathVariable UUID topicId) {
        NoteDto dto = noteFacade.generate(topicId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @PostMapping("/regenerate")
    public ResponseEntity<NoteDto> regenerate(@PathVariable UUID topicId) {
        NoteDto dto = noteFacade.regenerate(topicId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }
}
