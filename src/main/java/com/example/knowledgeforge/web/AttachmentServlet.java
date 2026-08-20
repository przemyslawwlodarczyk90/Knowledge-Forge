package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.attachment.Attachment;
import com.example.knowledgeforge.domain.attachment.AttachmentType;
import com.example.knowledgeforge.service.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Mapowany na /api/attachments/*. Lista i upload żyją w TopicServlet (zagnieżdżone pod
 * tematem — /api/topics/{id}/attachments), bo to zasób podrzędny w stosunku do tematu;
 * ten serwlet obsługuje operacje po samym id załącznika.
 *
 * Żaden z endpointów nie przyjmuje ścieżki pliku od klienta — plik jest zawsze
 * odnajdywany na podstawie {id} i rekordu z bazy (AttachmentService.resolveForStreaming),
 * który z kolei przepuszcza relativePath przez AttachmentStorage#resolveExisting
 * (normalize() + sprawdzenie, że ścieżka nie wychodzi poza attachments.storage.path).
 */
public class AttachmentServlet extends ApiServlet {

    private final AttachmentService attachmentService;

    public AttachmentServlet(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            writeJson(resp, 200, attachmentService.getById(parseLong(seg[0])));
        } else if (seg.length == 2 && "view".equals(seg[1])) {
            handleView(parseLong(seg[0]), req, resp);
        } else if (seg.length == 2 && "download".equals(seg[1])) {
            handleDownload(parseLong(seg[0]), resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            attachmentService.delete(parseLong(seg[0]), clientId(req));
            resp.setStatus(204);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ── GET /api/attachments/{id}/view — wyłącznie VIDEO, inline, z obsługą Range ──

    private void handleView(Long id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AttachmentService.ResolvedFile resolved = attachmentService.resolveForStreaming(id);
        Attachment a = resolved.attachment();

        if (a.getAttachmentType() != AttachmentType.VIDEO) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Attachment is not a playable video");
            return;
        }

        long fileSize = Files.size(resolved.path());
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setContentType("video/mp4");
        setContentDisposition(resp, "inline", a.getOriginalName());

        long[] range = parseRange(req.getHeader("Range"), fileSize);
        if (range == null) {
            // brak nagłówka Range (albo nie do sparsowania) -> cały plik, 200
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader("Content-Length", Long.toString(fileSize));
            if (!"HEAD".equalsIgnoreCase(req.getMethod())) {
                streamRange(resolved.path(), 0, fileSize - 1, resp.getOutputStream());
            }
            return;
        }
        if (range[0] < 0) {
            // start poza rozmiarem pliku -> 416
            resp.reset();
            resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            resp.setHeader("Content-Range", "bytes */" + fileSize);
            return;
        }

        long start = range[0];
        long end = range[1];
        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        resp.setHeader("Content-Length", Long.toString(end - start + 1));
        if (!"HEAD".equalsIgnoreCase(req.getMethod())) {
            streamRange(resolved.path(), start, end, resp.getOutputStream());
        }
    }

    // ── GET /api/attachments/{id}/download — dowolny typ, załącznik, cały plik strumieniowo ──

    private void handleDownload(Long id, HttpServletResponse resp) throws IOException {
        AttachmentService.ResolvedFile resolved = attachmentService.resolveForStreaming(id);
        Attachment a = resolved.attachment();
        long fileSize = Files.size(resolved.path());

        resp.setContentType(a.getContentType() != null ? a.getContentType() : "application/octet-stream");
        resp.setHeader("Content-Length", Long.toString(fileSize));
        setContentDisposition(resp, "attachment", a.getOriginalName());

        // Files.copy z InputStream do OutputStream kopiuje przez ograniczony bufor wewnętrznie —
        // nigdy nie materializuje całego pliku w pamięci.
        try (var in = Files.newInputStream(resolved.path())) {
            in.transferTo(resp.getOutputStream());
        }
    }

    // ── Range: bytes=start-end / bytes=-suffixLength / bytes=start- ──

    /**
     * Zwraca {start, end} (inclusive) albo null gdy brak/niesparsowalny nagłówek Range
     * (wtedy wywołujący ma zwrócić cały plik z 200), albo {-1, -1} gdy zakres jest poza
     * rozmiarem pliku (wywołujący ma zwrócić 416).
     */
    static long[] parseRange(String rangeHeader, long fileSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) return null;
        String spec = rangeHeader.substring("bytes=".length()).trim();
        if (spec.isEmpty() || spec.contains(",")) return null; // multi-range nieobsługiwane — serwuj cały plik

        int dash = spec.indexOf('-');
        if (dash < 0) return null;
        String startPart = spec.substring(0, dash);
        String endPart = spec.substring(dash + 1);

        try {
            long start;
            long end;
            if (startPart.isEmpty()) {
                // bytes=-N -> ostatnie N bajtów
                if (endPart.isEmpty()) return null;
                long suffixLength = Long.parseLong(endPart);
                if (suffixLength <= 0) return null;
                start = Math.max(0, fileSize - suffixLength);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(startPart);
                end = endPart.isEmpty() ? fileSize - 1 : Long.parseLong(endPart);
            }
            if (start < 0 || end < start) return null;
            if (start >= fileSize) return new long[]{-1, -1};
            end = Math.min(end, fileSize - 1);
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Kopiuje dokładnie [start, end] (inclusive) z pliku do strumienia, bez wczytywania go do pamięci. */
    private static void streamRange(Path path, long start, long end, OutputStream out) throws IOException {
        long remaining = end - start + 1;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.position(start);
            WritableByteChannel target = Channels.newChannel(out);
            while (remaining > 0) {
                long transferred = channel.transferTo(channel.position(), remaining, target);
                if (transferred <= 0) break;
                channel.position(channel.position() + transferred);
                remaining -= transferred;
            }
        }
    }

    /** RFC 5987 — bezpieczna nazwa dla starszych klientów (filename=) + poprawny UTF-8 (filename*=). */
    private static void setContentDisposition(HttpServletResponse resp, String type, String originalName) {
        String asciiFallback = originalName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
        resp.setHeader("Content-Disposition",
                type + "; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded);
    }
}
