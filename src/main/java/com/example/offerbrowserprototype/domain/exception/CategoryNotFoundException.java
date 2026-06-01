package com.example.offerbrowserprototype.domain.exception;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String id) {
        super("Category not found: " + id);
    }
}
