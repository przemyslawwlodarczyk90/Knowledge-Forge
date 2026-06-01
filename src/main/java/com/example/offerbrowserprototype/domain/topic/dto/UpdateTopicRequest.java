package com.example.offerbrowserprototype.domain.topic.dto;

import com.example.offerbrowserprototype.domain.topic.Difficulty;

public record UpdateTopicRequest(
        String title,
        String shortPrompt,
        Difficulty difficulty
) {
}
