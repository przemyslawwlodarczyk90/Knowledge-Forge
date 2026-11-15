package com.example.knowledgeforge.ai;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Zamienia ustrukturyzowaną odpowiedź modelu (NoteContent) na dokument edytora TipTap (ten sam
 * format JSON, który frontend zapisuje przez PUT /api/topics/{id}/note) — wynik można wprost
 * wczytać do edytora i zapisać zwykłym mechanizmem.
 */
public final class NoteContentDocumentMapper {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private NoteContentDocumentMapper() {
    }

    public static ObjectNode toDocument(NoteContent c) {
        ObjectNode doc = F.objectNode().put("type", "doc");
        ArrayNode content = doc.putArray("content");

        paragraphs(content, c.getSummary());

        if (notBlank(c.getSimpleExplanation())) {
            content.add(heading(2, "W prostych słowach"));
            paragraphs(content, c.getSimpleExplanation());
        }

        if (c.getSections() != null) {
            for (NoteContent.Section s : c.getSections()) {
                if (notBlank(s.getHeading())) content.add(heading(2, s.getHeading()));
                paragraphs(content, s.getContent());
            }
        }

        if (c.getExamples() != null && !c.getExamples().isEmpty()) {
            content.add(heading(2, "Przykłady"));
            for (NoteContent.Example e : c.getExamples()) {
                content.add(heading(3, exampleTitle(e)));
                if (notBlank(e.getContent())) {
                    if (e.getContent().contains("\n")) content.add(codeBlock(e.getContent()));
                    else content.add(paragraph(e.getContent()));
                }
                if (notBlank(e.getExplanation())) content.add(paragraph("Wniosek: " + e.getExplanation()));
            }
        }

        list(content, "Typowe błędy", c.getCommonMistakes());
        list(content, "Do zapamiętania", c.getMemoryPoints());
        list(content, "Szukaj dalej", c.getSuggestedSearchPhrases());
        return doc;
    }

    private static String exampleTitle(NoteContent.Example e) {
        String level = e.getLevel() == null ? null : switch (e.getLevel()) {
            case FOR_CHILD -> "dla początkujących";
            case CONTRASTIVE -> "porównanie";
            case ADVANCED -> "zaawansowany";
        };
        String title = notBlank(e.getTitle()) ? e.getTitle() : "Przykład";
        return level == null ? title : title + " (" + level + ")";
    }

    private static void list(ArrayNode content, String title, List<String> items) {
        if (items == null || items.stream().noneMatch(NoteContentDocumentMapper::notBlank)) return;
        content.add(heading(2, title));
        ObjectNode bullet = F.objectNode().put("type", "bulletList");
        ArrayNode li = bullet.putArray("content");
        for (String item : items) {
            if (!notBlank(item)) continue;
            ObjectNode listItem = F.objectNode().put("type", "listItem");
            listItem.putArray("content").add(paragraph(item));
            li.add(listItem);
        }
        content.add(bullet);
    }

    private static void paragraphs(ArrayNode content, String text) {
        if (!notBlank(text)) return;
        for (String part : text.split("\\n\\s*\\n|\\n")) {
            if (notBlank(part)) content.add(paragraph(part.strip()));
        }
    }

    private static ObjectNode heading(int level, String text) {
        ObjectNode h = F.objectNode().put("type", "heading");
        h.putObject("attrs").put("level", level);
        h.putArray("content").add(text(text));
        return h;
    }

    private static ObjectNode paragraph(String text) {
        ObjectNode p = F.objectNode().put("type", "paragraph");
        p.putArray("content").add(text(text));
        return p;
    }

    private static ObjectNode codeBlock(String text) {
        ObjectNode code = F.objectNode().put("type", "codeBlock");
        code.putArray("content").add(text(text));
        return code;
    }

    private static ObjectNode text(String text) {
        return F.objectNode().put("type", "text").put("text", text);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
