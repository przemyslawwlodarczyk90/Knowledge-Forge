package com.example.knowledgeforge.quiz;

/** Port generowania pytań przez AI — OpenAI (Spring AI) albo stub, gdy brak klucza. */
public interface QuizGenerationPort {

    /** Jedna porcja pytań — surowa odpowiedź modelu, walidowana dopiero przez {@link QuizDraftValidator}. */
    QuizBatch generate(QuizChunk chunk);

    String modelName();
}
