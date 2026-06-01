package com.example.offerbrowserprototype.domain.exception;

public class TopicNotFoundException extends NotFoundException {
    public TopicNotFoundException(String id) {
        super("Topic not found: " + id);
    }
}
