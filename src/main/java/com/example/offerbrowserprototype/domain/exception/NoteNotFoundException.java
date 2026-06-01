package com.example.offerbrowserprototype.domain.exception;

public class NoteNotFoundException extends NotFoundException {
    public NoteNotFoundException(String topicId) {
        super("Note not found for topic: " + topicId);
    }
}
