package com.example.knowledgeforge.domain.topic.dto;

import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.TopicType;

public record UpdateTopicRequest(
        String title,
        String shortPrompt,
        String author,
        DetailLevel detailLevel,
        TopicType type,
        /** Optimistic locking — TopicDto.version, na której użytkownik zaczął edycję. Gdy null,
         *  update jest bezwarunkowy (starsi/wewnętrzni wywołujący) — nowy frontend zawsze go wysyła. */
        Integer expectedVersion
) {
}
