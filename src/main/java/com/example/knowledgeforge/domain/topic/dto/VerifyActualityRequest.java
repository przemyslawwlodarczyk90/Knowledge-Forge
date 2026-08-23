package com.example.knowledgeforge.domain.topic.dto;

/** Body żądania POST /api/topics/{id}/verify-actuality — wyłącznie optimistic-locking version,
 *  celowo NIE ogólny boolean przyjmowany od frontendu (zob. dokumentacja/ACTUALITY_VERIFICATION.txt). */
public record VerifyActualityRequest(
        Integer expectedVersion
) {
}
