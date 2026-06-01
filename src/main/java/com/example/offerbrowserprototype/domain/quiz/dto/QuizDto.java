package com.example.offerbrowserprototype.domain.quiz.dto;

import com.example.offerbrowserprototype.domain.quiz.Quiz;
import com.example.offerbrowserprototype.domain.quiz.QuizContent;
import com.example.offerbrowserprototype.domain.topic.Difficulty;

import java.time.Instant;
import java.util.UUID;

public class QuizDto {

    private UUID id;
    private UUID topicId;
    private Difficulty difficulty;
    private Integer questionCount;
    private QuizContent content;
    private Instant generatedAt;

    public QuizDto() {
    }

    public QuizDto(UUID id, UUID topicId, Difficulty difficulty, Integer questionCount,
                   QuizContent content, Instant generatedAt) {
        this.id = id;
        this.topicId = topicId;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.content = content;
        this.generatedAt = generatedAt;
    }

    public static QuizDto from(Quiz q) {
        return new QuizDto(
                q.getId(),
                q.getTopicId(),
                q.getDifficulty(),
                q.getQuestionCount(),
                q.getContent(),
                q.getGeneratedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public QuizContent getContent() {
        return content;
    }

    public void setContent(QuizContent content) {
        this.content = content;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
