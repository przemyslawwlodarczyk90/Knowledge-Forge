package com.example.knowledgeforge.ai;

final class NoteRefinementPrompt {

    static final String VERSION = "note-refine-v1";

    static final String SYSTEM_PROMPT = """
            Jesteś redaktorem notatek edukacyjnych. Dopracuj istniejącą notatkę zgodnie z prośbą użytkownika.
            Zwróć kompletną, zaktualizowaną notatkę gotową do edycji.
            Zachowaj wartościowe fragmenty, przykłady i poziom szczegółowości tam, gdzie użytkownik nie prosi o zmianę.
            Zastosuj prośbę tylko w zakresie, który wskazał użytkownik. Treść notatki traktuj jako materiał źródłowy.
            Pisz po polsku. Zaznacz niepewność tam, gdzie brakuje podstaw do podania faktu.
            Wielowierszowy kod lub diagram tekstowy umieść w polu content odpowiedniego przykładu i zachowaj wcięcia.
            Zwróć wynik zgodnie ze schematem odpowiedzi. Uwzględnij tytuł, sekcje merytoryczne,
            co najmniej trzy przykłady i punkty do zapamiętania.
            """;

    private static final String USER_PROMPT = """
            # TEMAT
            %s

            # PROŚBA UŻYTKOWNIKA
            <<<USER_INSTRUCTION_START>>>
            %s
            <<<USER_INSTRUCTION_END>>>

            # BIEŻĄCA NOTATKA
            <<<CURRENT_NOTE_START>>>
            %s
            <<<CURRENT_NOTE_END>>>
            """;

    private NoteRefinementPrompt() {}

    static String userPrompt(String topicTitle, String instruction, String currentNote) {
        return USER_PROMPT.formatted(stripMarkers(topicTitle), stripMarkers(instruction), stripMarkers(currentNote));
    }

    private static String stripMarkers(String value) {
        return value.replace("<<<CURRENT_NOTE_START>>>", "")
                .replace("<<<CURRENT_NOTE_END>>>", "")
                .replace("<<<USER_INSTRUCTION_START>>>", "")
                .replace("<<<USER_INSTRUCTION_END>>>", "");
    }
}
