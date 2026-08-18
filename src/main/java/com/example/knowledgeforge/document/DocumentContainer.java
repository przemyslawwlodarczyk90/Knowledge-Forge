package com.example.knowledgeforge.document;

import com.example.knowledgeforge.json.JsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ".kfdoc" — własny, prosty format binarny dla treści notatek/instrukcji.
 * To zwykły ZIP (java.util.zip, biblioteka standardowa — zero zależności),
 * dokładnie ten sam pomysł co .docx/.odt:
 *
 *   manifest.json     — {"format":"kfdoc","version":1}
 *   content.json      — drzewo dokumentu edytora (ProseMirror/TipTap JSON)
 *   assets/&lt;id&gt;.png  — wklejone obrazki, referencjonowane z content.json
 *                        przez "asset:&lt;id&gt;.png" zamiast base64 w środku drzewa
 *
 * Całość trzymana jest jako jedna kolumna BYTEA w tabeli note.
 */
public final class DocumentContainer {

    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final String CONTENT_ENTRY = "content.json";
    private static final String ASSET_PREFIX = "assets/";
    public static final String ASSET_SCHEME = "asset:";

    private final JsonNode contentJson;
    private final Map<String, byte[]> assets;

    private DocumentContainer(JsonNode contentJson, Map<String, byte[]> assets) {
        this.contentJson = contentJson;
        this.assets = assets;
    }

    public static DocumentContainer of(JsonNode contentJson, Map<String, byte[]> assets) {
        return new DocumentContainer(contentJson, assets);
    }

    public static DocumentContainer empty() {
        return new DocumentContainer(JsonMapper.get().createObjectNode(), Map.of());
    }

    public JsonNode contentJson() {
        return contentJson;
    }

    public byte[] asset(String filename) {
        return assets.get(filename);
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos)) {
                putEntry(zip, MANIFEST_ENTRY, JsonMapper.get().writeValueAsBytes(Map.of("format", "kfdoc", "version", 1)));
                putEntry(zip, CONTENT_ENTRY, JsonMapper.get().writeValueAsBytes(contentJson));
                for (Map.Entry<String, byte[]> asset : assets.entrySet()) {
                    putEntry(zip, ASSET_PREFIX + asset.getKey(), asset.getValue());
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build .kfdoc container", e);
        }
    }

    public static DocumentContainer fromBytes(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) return empty();

        JsonNode content = JsonMapper.get().createObjectNode();
        Map<String, byte[]> assets = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (entry.getName().equals(CONTENT_ENTRY)) {
                    content = JsonMapper.get().readTree(data);
                } else if (entry.getName().startsWith(ASSET_PREFIX)) {
                    assets.put(entry.getName().substring(ASSET_PREFIX.length()), data);
                }
                zin.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .kfdoc container", e);
        }
        return new DocumentContainer(content, assets);
    }

    /**
     * Podmienia w miejscu wartości "src" węzłów obrazków (type == "image")
     * zaczynające się od {@link #ASSET_SCHEME} — używane przy odczycie, żeby
     * zamienić "asset:xyz.png" na realny, serwowalny URL.
     */
    public static void rewriteImageSrc(JsonNode node, Function<String, String> rewriter) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if ("image".equals(textOrNull(obj.get("type")))) {
                JsonNode attrs = obj.get("attrs");
                if (attrs != null && attrs.isObject()) {
                    ObjectNode attrsObj = (ObjectNode) attrs;
                    String src = textOrNull(attrsObj.get("src"));
                    if (src != null && src.startsWith(ASSET_SCHEME)) {
                        attrsObj.put("src", rewriter.apply(src.substring(ASSET_SCHEME.length())));
                    }
                }
            }
            obj.fields().forEachRemaining(e -> rewriteImageSrc(e.getValue(), rewriter));
        } else if (node.isArray()) {
            node.forEach(child -> rewriteImageSrc(child, rewriter));
        }
    }

    /** Cały tekst dokumentu jako jeden ciąg (węzły "text" połączone spacją) — do wyszukiwarki. */
    public String extractPlainText() {
        StringBuilder sb = new StringBuilder();
        collectText(contentJson, sb);
        return sb.toString();
    }

    private static void collectText(JsonNode node, StringBuilder sb) {
        if (node == null) return;
        if (node.isObject()) {
            if ("text".equals(textOrNull(node.get("type")))) {
                String text = textOrNull(node.get("text"));
                if (text != null) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(text);
                }
            }
            node.fields().forEachRemaining(e -> collectText(e.getValue(), sb));
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, sb));
        }
    }

    private static String textOrNull(JsonNode n) {
        return n != null && n.isTextual() ? n.asText() : null;
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
