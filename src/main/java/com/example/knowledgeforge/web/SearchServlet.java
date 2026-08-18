package com.example.knowledgeforge.web;

import com.example.knowledgeforge.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Mapowany na /api/search — GET /api/search?q=... */
public class SearchServlet extends ApiServlet {

    private final SearchService searchService;

    public SearchServlet(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String q = req.getParameter("q");
        writeJson(resp, 200, searchService.search(q));
    }
}
