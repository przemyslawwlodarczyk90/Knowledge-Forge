package com.example.knowledgeforge.domain.exception;

public class TopicNotFoundException extends NotFoundException {
    public TopicNotFoundException(String id) {
        super("Topic not found: " + id);
    }
}
