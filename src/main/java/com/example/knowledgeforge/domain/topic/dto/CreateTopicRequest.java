package com.example.knowledgeforge.domain.topic.dto;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;

import java.util.UUID;

public record CreateTopicRequest(
        UUID categoryId,
        String title,
        String shortPrompt,
        String author,
        DetailLevel detailLevel,
        TopicType type
) {
}
