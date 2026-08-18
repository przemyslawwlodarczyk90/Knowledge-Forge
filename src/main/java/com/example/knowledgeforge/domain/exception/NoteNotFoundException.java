package com.example.knowledgeforge.domain.exception;

public class NoteNotFoundException extends NotFoundException {
    public NoteNotFoundException(String topicId) {
        super("Note not found for topic: " + topicId);
    }
}
