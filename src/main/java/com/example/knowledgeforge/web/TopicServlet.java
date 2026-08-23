package com.example.knowledgeforge.web;

import com.example.knowledgeforge.actuality.ActualityVerificationService;
import com.example.knowledgeforge.domain.exception.PayloadTooLargeException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.UpdateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.VerifyActualityRequest;
import com.example.knowledgeforge.service.AttachmentService;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Mapowany na /api/topics/*. Obsługuje CRUD tematu oraz zagnieżdżony
 * podzasób notatki/instrukcji: /api/topics/{id}/note (treść edytora),
 * /note/assets/{plik} (wklejone obrazki). PDF generuje się wyłącznie
 * w przeglądarce, na żądanie — brak tu po niego endpointu.
 * GET /api/topics?author=&detailLevel=&categoryId= — filtr do panelu "Filtry".
 * GET /api/topics/authors — lista autorów do rozwijanego wyboru w filtrze.
 * GET /api/topics/actuality-review[?author=] — tematy wymagające sprawdzenia aktualności
 * (zob. dokumentacja/ACTUALITY_VERIFICATION.txt). POST /api/topics/{id}/verify-actuality — ich ręczne
 * potwierdzenie. Obie ścieżki sprawdzane PRZED ogólną obsługą /api/topics/{id}, żeby
 * "actuality-review" nie trafiło do parseUuid(...).
 */
public class TopicServlet extends ApiServlet {

    private static final Map<String, String> ASSET_CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private final TopicService topicService;
    private final NoteService noteService;
    private final AttachmentService attachmentService;
    private final ActualityVerificationService actualityVerificationService;

    public TopicServlet(TopicService topicService, NoteService noteService, AttachmentService attachmentService,
                         ActualityVerificationService actualityVerificationService) {
        this.topicService = topicService;
        this.noteService = noteService;
        this.attachmentService = attachmentService;
        this.actualityVerificationService = actualityVerificationService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 0) {
            writeJson(resp, 200, topicService.filter(
                    blankToNull(req.getParameter("author")),
                    parseDetailLevel(req.getParameter("detailLevel")),
                    blankToNull(req.getParameter("categoryId")) == null ? null : parseUuid(req.getParameter("categoryId"))
            ));
        } else if (seg.length == 1 && "authors".equals(seg[0])) {
            writeJson(resp, 200, topicService.listAuthors());
        } else if (seg.length == 1 && "actuality-review".equals(seg[0])) {
            // Sprawdzane PRZED ogólnym "seg.length == 1 -> getById" niżej — inaczej "actuality-review"
            // trafiłoby do parseUuid(...) i zwróciło 400 zamiast właściwej listy.
            writeJson(resp, 200, actualityVerificationService.listForReview(blankToNull(req.getParameter("author"))));
        } else if (seg.length == 1) {
            writeJson(resp, 200, topicService.getById(parseUuid(seg[0])));
        } else if (seg.length == 2 && "note".equals(seg[1])) {
            writeJson(resp, 200, noteService.getByTopicId(parseUuid(seg[0])));
        } else if (seg.length == 4 && "note".equals(seg[1]) && "assets".equals(seg[2])) {
            byte[] data = noteService.getAsset(parseUuid(seg[0]), seg[3]);
            writeBinary(resp, 200, contentTypeFor(seg[3]), data);
        } else if (seg.length == 2 && "attachments".equals(seg[1])) {
            writeJson(resp, 200, attachmentService.listByTopic(parseUuid(seg[0])));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 0) {
            var dto = topicService.create(readJson(req, CreateTopicRequest.class), clientId(req));
            writeJson(resp, 201, dto);
        } else if (seg.length == 2 && "attachments".equals(seg[1])) {
            handleUploadAttachment(parseUuid(seg[0]), req, resp);
        } else if (seg.length == 2 && "verify-actuality".equals(seg[1])) {
            var body = readJson(req, VerifyActualityRequest.class);
            var dto = actualityVerificationService.confirmActuality(parseUuid(seg[0]), body.expectedVersion(), clientId(req));
            writeJson(resp, 200, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * multipart/form-data: pole "file" (wymagane) + opcjonalny "description". MultipartConfig
     * jest zarejestrowany na tym serwlecie w Main.java (fileSizeThreshold=0 -> Jetty od razu
     * spilluje część na dysk zamiast buforować duży plik w pamięci); AttachmentService dalej
     * kopiuje ją strumieniowo do docelowego katalogu, licząc SHA-256 w locie.
     */
    private void handleUploadAttachment(UUID topicId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (IllegalStateException e) {
            // Jetty rzuca to, gdy część przekracza MultipartConfigElement#getMaxFileSize()
            throw new PayloadTooLargeException("Uploaded file exceeds the configured size limit");
        } catch (Exception e) {
            throw new ValidationException("Malformed multipart upload: " + e.getMessage());
        }
        if (filePart == null || filePart.getSize() == 0) {
            throw new ValidationException("Missing or empty 'file' part");
        }
        String originalName = filePart.getSubmittedFileName();
        if (originalName == null || originalName.isBlank()) {
            throw new ValidationException("Missing file name");
        }

        String description = readOptionalTextPart(req, "description");

        log.info(() -> "Attachment upload starting: topic=" + topicId + " name='" + originalName
                + "' declaredSize=" + filePart.getSize() + " declaredContentType=" + filePart.getContentType());

        try (InputStream in = filePart.getInputStream()) {
            var dto = attachmentService.upload(topicId, originalName, filePart.getContentType(), in, description, clientId(req));
            writeJson(resp, 201, dto);
        }
    }

    private String readOptionalTextPart(HttpServletRequest req, String name) throws IOException {
        Part part;
        try {
            part = req.getPart(name);
        } catch (Exception e) {
            return null;
        }
        if (part == null) return null;
        try (InputStream in = part.getInputStream()) {
            return new String(in.readNBytes(4096), StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 2 && "note".equals(seg[1])) {
            var dto = noteService.save(parseUuid(seg[0]), readJson(req, SaveNoteRequest.class), clientId(req));
            writeJson(resp, 200, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            UUID id = parseUuid(seg[0]);
            var dto = topicService.update(id, readJson(req, UpdateTopicRequest.class), clientId(req));
            writeJson(resp, 200, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            topicService.delete(parseUuid(seg[0]), clientId(req));
            resp.setStatus(204);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String contentTypeFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
        return ASSET_CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private DetailLevel parseDetailLevel(String s) {
        String v = blankToNull(s);
        return v == null ? null : DetailLevel.valueOf(v);
    }
}
