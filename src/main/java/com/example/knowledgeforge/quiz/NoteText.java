package com.example.knowledgeforge.quiz;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Tekst notatki (dokument TipTap) dla promptu quizu. W odróżnieniu od
 * DocumentContainer#extractPlainText (wyszukiwarka — wszystko w jednej linii) zachowuje
 * strukturę: nagłówki, akapity, punkty listy i bloki kodu z wcięciami — model musi widzieć kod
 * dokładnie tak, jak w notatce, żeby układać o nim pytania.
 */
public final class NoteText {

    private static final Set<String> BLOCKS = Set.of("paragraph", "heading", "blockquote", "listItem", "horizontalRule");

    private NoteText() {
    }

    public static String of(JsonNode doc) {
        StringBuilder sb = new StringBuilder();
        walk(doc, sb);
        return sb.toString().replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static void walk(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) return;
        if (node.isArray()) {
            node.forEach(child -> walk(child, sb));
            return;
        }
        if (!node.isObject()) return;

        String type = node.path("type").asText("");
        switch (type) {
            case "text" -> sb.append(node.path("text").asText(""));
            case "hardBreak" -> sb.append('\n');
            case "codeBlock" -> {
                String lang = node.path("attrs").path("language").asText("");
                sb.append("\n```").append(lang).append('\n');
                walk(node.get("content"), sb);
                sb.append("\n```\n\n");
            }
            case "heading" -> {
                sb.append("\n").append("#".repeat(Math.max(1, node.path("attrs").path("level").asInt(2)))).append(' ');
                walk(node.get("content"), sb);
                sb.append("\n\n");
            }
            case "listItem" -> {
                sb.append("- ");
                walk(node.get("content"), sb);
                trimTrailingNewlines(sb);
                sb.append('\n');
            }
            default -> {
                walk(node.get("content"), sb);
                if (BLOCKS.contains(type)) sb.append("\n\n");
            }
        }
    }

    private static void trimTrailingNewlines(StringBuilder sb) {
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.setLength(sb.length() - 1);
    }
}
