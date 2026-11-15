package com.example.knowledgeforge.web;

import com.example.knowledgeforge.domain.search.SearchResultDto;
import com.example.knowledgeforge.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/search?q=... */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public List<SearchResultDto> search(@RequestParam(value = "q", required = false) String q) {
        return searchService.search(q);
    }
}
