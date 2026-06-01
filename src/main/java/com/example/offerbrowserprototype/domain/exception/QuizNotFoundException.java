package com.example.offerbrowserprototype.domain.exception;

public class QuizNotFoundException extends NotFoundException {
    public QuizNotFoundException(String topicId) {
        super("Quiz not found for topic: " + topicId);
    }
}
