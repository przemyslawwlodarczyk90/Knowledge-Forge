package com.example.knowledgeforge.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Serwuje zbudowany frontend z classpath:/static (spakowany do JAR-a razem
 * z klasami). Pliki z kropką w nazwie (main.js, index.css, favicon.svg) są
 * serwowane wprost; wszystko inne (trasy React Router, np. /dashboard) trafia
 * do index.html — zastępuje dawny SpaController + Spring resource handler.
 */
public class SpaServlet extends HttpServlet {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html;charset=UTF-8"),
            Map.entry("js", "application/javascript;charset=UTF-8"),
            Map.entry("css", "text/css;charset=UTF-8"),
            Map.entry("json", "application/json;charset=UTF-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("map", "application/json;charset=UTF-8")
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        if (path == null || path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }

        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        boolean looksLikeFile = lastSegment.contains(".");

        String resourcePath = "/static" + path;
        InputStream in = looksLikeFile ? getClass().getResourceAsStream(resourcePath) : null;

        if (in == null) {
            // SPA route (no dot) or unknown static path -> fall back to index.html
            resourcePath = "/static/index.html";
            in = getClass().getResourceAsStream(resourcePath);
            if (in == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        resp.setContentType(contentTypeFor(resourcePath));
        try (InputStream input = in; OutputStream out = resp.getOutputStream()) {
            input.transferTo(out);
        }
    }

    private String contentTypeFor(String resourcePath) {
        int dot = resourcePath.lastIndexOf('.');
        String ext = dot >= 0 ? resourcePath.substring(dot + 1).toLowerCase() : "";
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
