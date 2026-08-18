package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.note.dto.SaveNoteRequest;
import com.example.knowledgeforge.domain.topic.dto.CreateTopicRequest;
import com.example.knowledgeforge.domain.topic.dto.UpdateTopicRequest;
import com.example.knowledgeforge.service.NoteService;
import com.example.knowledgeforge.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Mapowany na /api/topics/*. Obsługuje CRUD tematu oraz zagnieżdżony
 * podzasób notatki/instrukcji: /api/topics/{id}/note (treść edytora),
 * /note/assets/{plik} (wklejone obrazki). PDF generuje się wyłącznie
 * w przeglądarce, na żądanie — brak tu po niego endpointu.
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

    public TopicServlet(TopicService topicService, NoteService noteService) {
        this.topicService = topicService;
        this.noteService = noteService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            writeJson(resp, 200, topicService.getById(parseUuid(seg[0])));
        } else if (seg.length == 2 && "note".equals(seg[1])) {
            writeJson(resp, 200, noteService.getByTopicId(parseUuid(seg[0])));
        } else if (seg.length == 4 && "note".equals(seg[1]) && "assets".equals(seg[2])) {
            byte[] data = noteService.getAsset(parseUuid(seg[0]), seg[3]);
            writeBinary(resp, 200, contentTypeFor(seg[3]), data);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 0) {
            var dto = topicService.create(readJson(req, CreateTopicRequest.class));
            writeJson(resp, 201, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 2 && "note".equals(seg[1])) {
            var dto = noteService.save(parseUuid(seg[0]), readJson(req, SaveNoteRequest.class));
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
            var dto = topicService.update(id, readJson(req, UpdateTopicRequest.class));
            writeJson(resp, 200, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            topicService.delete(parseUuid(seg[0]));
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
}
