package com.example.knowledgeforge.domain.exception;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String id) {
        super("Category not found: " + id);
    }
}
