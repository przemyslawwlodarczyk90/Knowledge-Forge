package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.domain.topic.dto.CreateTopicRequest;
import com.example.offerbrowserprototype.domain.topic.dto.TopicDto;
import com.example.offerbrowserprototype.domain.topic.dto.UpdateTopicRequest;
import com.example.offerbrowserprototype.infrastructure.facade.TopicFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TopicController {

    private final TopicFacade topicFacade;

    @PostMapping("/topics")
    public ResponseEntity<TopicDto> create(@Valid @RequestBody CreateTopicRequest request) {
        TopicDto dto = topicFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/topics/{id}")
    public ResponseEntity<TopicDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(topicFacade.getById(id));
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ResponseEntity<List<TopicDto>> listByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(topicFacade.listByCategory(categoryId));
    }

    @PatchMapping("/topics/{id}")
    public ResponseEntity<TopicDto> update(
            @PathVariable UUID id,
            @RequestBody UpdateTopicRequest request) {
        return ResponseEntity.ok(topicFacade.update(id, request));
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        topicFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
