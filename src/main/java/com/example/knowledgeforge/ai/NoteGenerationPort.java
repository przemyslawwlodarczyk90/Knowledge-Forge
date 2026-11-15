package com.example.knowledgeforge.ai;

/** Port generowania notatki przez AI — implementacja OpenAI (Spring AI) albo stub, gdy brak klucza. */
public interface NoteGenerationPort {

    /**
     * @param topicTitle      tytuł tematu
     * @param userInstruction dodatkowe wskazówki użytkownika (np. opis tematu) — mogą być puste
     * @param difficulty      głębokość notatki
     */
    NoteContent generate(String topicTitle, String userInstruction, NoteDifficulty difficulty);

    /** Nazwa modelu (do informacji w odpowiedzi API). */
    String modelName();
}
