package com.example.knowledgeforge.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jedna, współdzielona instancja Jacksona dla całej aplikacji — używana
 * zarówno przez serwlety (ciało żądania/odpowiedzi HTTP) jak i DAO
 * (kolumny *_json w bazie). Jackson to biblioteka (de)serializacji,
 * nie framework — nie zarządza cyklem życia obiektów.
 */
public final class JsonMapper {

    private static final ObjectMapper INSTANCE = build();

    private JsonMapper() {
    }

    public static ObjectMapper get() {
        return INSTANCE;
    }

    private static ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
