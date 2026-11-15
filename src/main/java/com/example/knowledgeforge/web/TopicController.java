package com.example.knowledgeforge.web;

import com.example.knowledgeforge.actuality.ActualityVerificationService;
import com.example.knowledgeforge.domain.attachment.dto.AttachmentDto;
import com.example.knowledgeforge.domain.exception.PayloadTooLargeException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.note.dto.NoteDto;
import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.DetailLevel;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.TopicDto;
import com.example.knowledgeforge.domain.topic.dto.UpdateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.VerifyActualityRequest;
import com.example.knowledgeforge.service.AttachmentService;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static com.example.knowledgeforge.web.ApiSupport.blankToNull;
import static com.example.knowledgeforge.web.ApiSupport.clientId;
import static com.example.knowledgeforge.web.ApiSupport.parseUuid;

/**
 * /api/topics — CRUD tematu oraz zagnieżdżony podzasób notatki/instrukcji: /api/topics/{id}/note
 * (treść edytora), /note/assets/{plik} (wklejone obrazki). PDF generuje się wyłącznie w przeglądarce.
 * GET /api/topics?author=&detailLevel=&categoryId= — filtr do panelu "Filtry".
 * GET /api/topics/authors — lista autorów do filtra.
 * GET /api/topics/actuality-review[?author=] — tematy wymagające sprawdzenia aktualności
 * (zob. dokumentacja/ACTUALITY_VERIFICATION.txt). POST /api/topics/{id}/verify-actuality — ręczne
 * potwierdzenie. Stałe ścieżki ("authors", "actuality-review") mają w Spring MVC pierwszeństwo
 * przed /{id}, więc nigdy nie trafiają do parseUuid(...).
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private static final Logger log = Logger.getLogger(TopicController.class.getName());

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

    public TopicController(TopicService topicService, NoteService noteService, AttachmentService attachmentService,
                           ActualityVerificationService actualityVerificationService) {
        this.topicService = topicService;
        this.noteService = noteService;
        this.attachmentService = attachmentService;
        this.actualityVerificationService = actualityVerificationService;
    }

    // ── GET ──────────────────────────────────────────────────────────

    @GetMapping
    public List<TopicDto> filter(@RequestParam(value = "author", required = false) String author,
                                 @RequestParam(value = "detailLevel", required = false) String detailLevel,
                                 @RequestParam(value = "categoryId", required = false) String categoryId) {
        return topicService.filter(
                blankToNull(author),
                parseDetailLevel(detailLevel),
                blankToNull(categoryId) == null ? null : parseUuid(categoryId));
    }

    @GetMapping("/authors")
    public List<String> authors() {
        return topicService.listAuthors();
    }

    @GetMapping("/actuality-review")
    public List<TopicDto> actualityReview(@RequestParam(value = "author", required = false) String author) {
        return actualityVerificationService.listForReview(blankToNull(author));
    }

    @GetMapping("/{id}")
    public TopicDto getById(@PathVariable String id) {
        return topicService.getById(parseUuid(id));
    }

    @GetMapping("/{id}/note")
    public NoteDto getNote(@PathVariable String id) {
        return noteService.getByTopicId(parseUuid(id));
    }

    @GetMapping("/{id}/note/assets/{filename}")
    public ResponseEntity<byte[]> getAsset(@PathVariable String id, @PathVariable String filename) {
        byte[] data = noteService.getAsset(parseUuid(id), filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentTypeFor(filename)))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/{id}/attachments")
    public List<AttachmentDto> attachments(@PathVariable String id) {
        return attachmentService.listByTopic(parseUuid(id));
    }

    // ── POST ─────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicDto create(@RequestBody CreateTopicRequest body, HttpServletRequest req) {
        return topicService.create(body, clientId(req));
    }

    @PostMapping("/{id}/verify-actuality")
    public TopicDto verifyActuality(@PathVariable String id, @RequestBody VerifyActualityRequest body,
                                    HttpServletRequest req) {
        return actualityVerificationService.confirmActuality(parseUuid(id), body.expectedVersion(), clientId(req));
    }

    /**
     * multipart/form-data: pole "file" (wymagane) + opcjonalny "description". MultipartConfigElement
     * (zob. config.ApplicationBeans) ma fileSizeThreshold=0 -> kontener od razu spilluje część na dysk
     * zamiast buforować duży plik w pamięci; AttachmentService dalej kopiuje ją strumieniowo do
     * docelowego katalogu, licząc SHA-256 w locie.
     */
    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto uploadAttachment(@PathVariable String id, HttpServletRequest req) throws IOException {
        UUID topicId = parseUuid(id);
        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (IllegalStateException e) {
            // kontener rzuca to, gdy część przekracza MultipartConfigElement#getMaxFileSize()
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
            return attachmentService.upload(topicId, originalName, filePart.getContentType(), in, description, clientId(req));
        }
    }

    // ── PUT / PATCH / DELETE ────────────────────────────────────────

    @PutMapping("/{id}/note")
    public NoteDto saveNote(@PathVariable String id, @RequestBody SaveNoteRequest body, HttpServletRequest req) {
        return noteService.save(parseUuid(id), body, clientId(req));
    }

    @PatchMapping("/{id}")
    public TopicDto update(@PathVariable String id, @RequestBody UpdateTopicRequest body, HttpServletRequest req) {
        return topicService.update(parseUuid(id), body, clientId(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, HttpServletRequest req) {
        topicService.delete(parseUuid(id), clientId(req));
    }

    // ── Pomocnicze ──────────────────────────────────────────────────

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

    private String contentTypeFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
        return ASSET_CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private DetailLevel parseDetailLevel(String s) {
        String v = blankToNull(s);
        return v == null ? null : DetailLevel.valueOf(v);
    }
}
