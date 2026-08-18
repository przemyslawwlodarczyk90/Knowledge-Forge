package com.example.knowledgeforge.domain.note.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Payload zapisu notatki wysyłany przez edytor: drzewo dokumentu (obrazki
 * odnoszą się do assetów przez "asset:&lt;id&gt;") i same assety w base64.
 * PDF nie jest tu w ogóle — generuje się on-demand w przeglądarce, dopiero
 * gdy ktoś naprawdę kliknie "Pobierz PDF", zamiast puchnąć na dysku przy
 * każdym zapisie każdej notatki.
 */
public record SaveNoteRequest(
        JsonNode contentJson,
        List<AssetPayload> assets
) {
    public record AssetPayload(String id, String mimeType, String dataBase64) {
    }
}
