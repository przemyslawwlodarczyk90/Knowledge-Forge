package com.example.knowledgeforge.domain.search;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;

import java.util.UUID;

public record SearchResultDto(
        UUID topicId,
        UUID categoryId,
        String title,
        TopicType type,
        DetailLevel detailLevel,
        String matchedIn, // "TITLE" | "DESCRIPTION" | "CONTENT"
        String snippet
) {
}
