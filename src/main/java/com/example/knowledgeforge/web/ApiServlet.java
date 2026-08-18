package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.ForbiddenException;
import com.example.knowledgeforge.domain.exception.NotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.json.JsonMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bazowy serwlet dla zasobów JSON — zastępuje to, co dawał Spring MVC:
 * routing na podstawie ścieżki/metody, (de)serializację ciała żądania
 * i mapowanie wyjątków domenowych na kody HTTP (dawny GlobalExceptionHandler).
 *
 * HttpServlet nie ma wbudowanego dopatch(), więc dispatch robimy sami
 * w service() i wołamy własną metodę doPatch tam, gdzie jest potrzebna.
 */
public abstract class ApiServlet extends HttpServlet {

    protected static final Logger log = Logger.getLogger(ApiServlet.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.fine(() -> req.getMethod() + " " + req.getRequestURI());
        try {
            switch (req.getMethod()) {
                case "GET" -> handle(req, resp, () -> doGet(req, resp));
                case "POST" -> handle(req, resp, () -> doPost(req, resp));
                case "PUT" -> handle(req, resp, () -> doPut(req, resp));
                case "PATCH" -> handle(req, resp, () -> doPatch(req, resp));
                case "DELETE" -> handle(req, resp, () -> doDelete(req, resp));
                case "OPTIONS" -> resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (RuntimeException e) {
            // handle() już przechwyciło znane wyjątki; to tylko ostatnia siatka
            writeError(resp, 500, "Unexpected error: " + e.getMessage());
            log.log(Level.SEVERE, "Unhandled error in " + req.getMethod() + " " + req.getRequestURI(), e);
        }
    }

    /** Domyślne no-op — konkretny serwlet nadpisuje metody, których faktycznie używa. */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @FunctionalInterface
    private interface Action {
        void run() throws IOException;
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp, Action action) throws IOException {
        try {
            action.run();
        } catch (NotFoundException e) {
            writeError(resp, 404, e.getMessage());
        } catch (ConflictException e) {
            writeError(resp, 409, e.getMessage());
        } catch (ForbiddenException e) {
            writeError(resp, 403, e.getMessage());
        } catch (ValidationException e) {
            writeError(resp, 400, e.getMessage());
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error handling " + req.getMethod() + " " + req.getRequestURI(), e);
            writeError(resp, 500, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // ── Pomocnicze — JSON i ścieżka ─────────────────────────────────

    protected <T> T readJson(HttpServletRequest req, Class<T> type) throws IOException {
        try (var in = req.getInputStream()) {
            return JsonMapper.get().readValue(in, type);
        } catch (IOException e) {
            throw new ValidationException("Malformed JSON body: " + e.getMessage());
        }
    }

    protected void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        if (body != null) {
            JsonMapper.get().writeValue(resp.getOutputStream(), body);
        }
    }

    protected void writeBinary(HttpServletResponse resp, int status, String contentType, byte[] data) throws IOException {
        resp.setStatus(status);
        resp.setContentType(contentType);
        resp.setContentLength(data.length);
        resp.getOutputStream().write(data);
    }

    protected void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        writeJson(resp, status, Map.of("message", message == null ? "" : message));
    }

    /** Segmenty ścieżki po ścieżce serwletu, bez wiodącego/końcowego '/'. Np. "/abc/def" -> ["abc","def"]. */
    protected String[] pathSegments(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return new String[0];
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed.split("/");
    }

    protected UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + value);
        }
    }
}
