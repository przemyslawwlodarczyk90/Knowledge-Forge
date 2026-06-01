package com.example.offerbrowserprototype.domain.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class QuizContentConverter implements AttributeConverter<QuizContent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(QuizContent attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting QuizContent to JSON", e);
        }
    }

    @Override
    public QuizContent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, QuizContent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to QuizContent", e);
        }
    }
}
