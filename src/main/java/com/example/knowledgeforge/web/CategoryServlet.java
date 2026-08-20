package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.category.dto.CreateCategoryRequest;
import com.example.knowledgeforge.domain.category.dto.UpdateCategoryRequest;
import com.example.knowledgeforge.service.CategoryService;
import com.example.knowledgeforge.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/** Mapowany na /api/categories/*. Zawiera też odczyt tematów danej kategorii. */
public class CategoryServlet extends ApiServlet {

    private final CategoryService categoryService;
    private final TopicService topicService;

    public CategoryServlet(CategoryService categoryService, TopicService topicService) {
        this.categoryService = categoryService;
        this.topicService = topicService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1 && "tree".equals(seg[0])) {
            writeJson(resp, 200, categoryService.getTree());
        } else if (seg.length == 2 && "topics".equals(seg[1])) {
            writeJson(resp, 200, topicService.listByCategory(parseUuid(seg[0])));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 0) {
            var dto = categoryService.create(readJson(req, CreateCategoryRequest.class), clientId(req));
            writeJson(resp, 201, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            UUID id = parseUuid(seg[0]);
            var dto = categoryService.update(id, readJson(req, UpdateCategoryRequest.class), clientId(req));
            writeJson(resp, 200, dto);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] seg = pathSegments(req);
        if (seg.length == 1) {
            categoryService.delete(parseUuid(seg[0]), clientId(req));
            resp.setStatus(204);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
