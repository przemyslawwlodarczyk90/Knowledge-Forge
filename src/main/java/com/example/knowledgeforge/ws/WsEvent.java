package com.example.knowledgeforge.ws;

import java.time.Instant;

/**
 * Stabilna koperta zdarzenia WebSocket — ten sam kształt dla każdego typu zdarzenia,
 * payload niesie dane specyficzne dla typu (zob. ApplicationEventHub).
 * sourceClientId pozwala karcie przeglądarki, która sama wykonała operację, rozpoznać
 * i zignorować własne zdarzenie (zob. X-Client-Id po stronie frontendu).
 */
public record WsEvent(
        String eventId,
        String type,
        String entityId,
        String categoryId,
        String actorId,
        String sourceClientId,
        Instant occurredAt,
        Object payload
) {
}
