package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.category.dto.CategoryDto;
import com.example.knowledgeforge.domain.category.dto.CategoryTreeDto;
import com.example.knowledgeforge.domain.category.dto.CreateCategoryRequest;
import com.example.knowledgeforge.domain.category.dto.UpdateCategoryRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.service.CategoryService;
import com.example.knowledgeforge.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.knowledgeforge.web.ApiSupport.clientId;
import static com.example.knowledgeforge.web.ApiSupport.parseUuid;

/** /api/categories — zawiera też odczyt tematów danej kategorii. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final TopicService topicService;

    public CategoryController(CategoryService categoryService, TopicService topicService) {
        this.categoryService = categoryService;
        this.topicService = topicService;
    }

    @GetMapping("/tree")
    public CategoryTreeDto tree() {
        return categoryService.getTree();
    }

    @GetMapping("/{id}/topics")
    public List<TopicDto> topics(@PathVariable String id) {
        return topicService.listByCategory(parseUuid(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody CreateCategoryRequest body, HttpServletRequest req) {
        return categoryService.create(body, clientId(req));
    }

    @PatchMapping("/{id}")
    public CategoryDto update(@PathVariable String id, @RequestBody UpdateCategoryRequest body, HttpServletRequest req) {
        return categoryService.update(parseUuid(id), body, clientId(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest req) {
        categoryService.delete(parseUuid(id), clientId(req));
    }
}
