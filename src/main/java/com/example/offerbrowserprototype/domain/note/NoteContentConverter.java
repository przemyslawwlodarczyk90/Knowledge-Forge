package com.example.offerbrowserprototype.domain.note;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NoteContentConverter implements AttributeConverter<NoteContent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(NoteContent attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting NoteContent to JSON", e);
        }
    }

    @Override
    public NoteContent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, NoteContent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to NoteContent", e);
        }
    }
}
