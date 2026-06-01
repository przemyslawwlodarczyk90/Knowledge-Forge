package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.domain.category.dto.CategoryDto;
import com.example.offerbrowserprototype.domain.category.dto.CategoryTreeDto;
import com.example.offerbrowserprototype.domain.category.dto.CreateCategoryRequest;
import com.example.offerbrowserprototype.domain.category.dto.UpdateCategoryRequest;
import com.example.offerbrowserprototype.infrastructure.facade.CategoryFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryFacade categoryFacade;

    @GetMapping("/tree")
    public ResponseEntity<CategoryTreeDto> getTree() {
        return ResponseEntity.ok(categoryFacade.getTree());
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto dto = categoryFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryFacade.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
