package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.document.DocumentContainer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoteContentDocumentMapperTest {

    static NoteContent full() {
        NoteContent c = new NoteContent();
        c.setTitle("Rekurencja");
        c.setSummary("Akapit pierwszy.\n\nAkapit drugi.");
        c.setSimpleExplanation("Jak lustro w lustrze.");
        NoteContent.Section s = new NoteContent.Section();
        s.setHeading("Warunek stopu");
        s.setContent("Bez niego stos się przepełni.");
        c.setSections(List.of(s));
        NoteContent.Example kid = example("Lalki", NoteContent.ExampleLevel.FOR_CHILD, "Matrioszka w matrioszce", "Każda mniejsza");
        NoteContent.Example code = example("Silnia", NoteContent.ExampleLevel.ADVANCED, "int f(int n) {\n  return n * f(n-1);\n}", "Brak stopu");
        NoteContent.Example contrast = example(null, NoteContent.ExampleLevel.CONTRASTIVE, "Pętla vs rekurencja", null);
        c.setExamples(List.of(kid, code, contrast));
        c.setCommonMistakes(List.of("Brak warunku stopu", " "));
        c.setMemoryPoints(List.of("Stos ma limit"));
        c.setSuggestedSearchPhrases(null);
        return c;
    }

    static NoteContent.Example example(String title, NoteContent.ExampleLevel level, String content, String explanation) {
        NoteContent.Example e = new NoteContent.Example();
        e.setTitle(title);
        e.setLevel(level);
        e.setContent(content);
        e.setExplanation(explanation);
        return e;
    }

    @Test
    void produces_tiptap_document_with_all_sections() {
        JsonNode doc = NoteContentDocumentMapper.toDocument(full());
        assertThat(doc.get("type").asText()).isEqualTo("doc");
        List<String> types = new ArrayList<>();
        doc.get("content").forEach(n -> types.add(n.get("type").asText()));
        assertThat(types).contains("paragraph", "heading", "codeBlock", "bulletList");
        assertThat(types).hasSizeGreaterThan(10);

        String text = doc.toString();
        assertThat(text).contains("W prostych słowach", "Warunek stopu", "Przykłady", "Lalki (dla początkujących)",
                "Przykład (porównanie)", "Silnia (zaawansowany)", "Wniosek: Każda mniejsza", "Typowe błędy",
                "Do zapamiętania").doesNotContain("Szukaj dalej");
    }

    @Test
    void document_is_readable_by_note_container_plain_text_extraction() {
        JsonNode doc = NoteContentDocumentMapper.toDocument(full());
        String plain = DocumentContainer.of(doc, Map.of(), Map.of()).extractPlainText();
        assertThat(plain).contains("Akapit drugi").contains("Stos ma limit");
    }

    @Test
    void empty_content_yields_empty_document() {
        JsonNode doc = NoteContentDocumentMapper.toDocument(new NoteContent());
        assertThat(doc.get("content")).isEmpty();
    }
}
