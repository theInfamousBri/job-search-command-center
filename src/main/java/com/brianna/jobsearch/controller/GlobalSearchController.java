package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.search.GlobalSearchResponse;
import com.brianna.jobsearch.service.GlobalSearchService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public ResponseEntity<GlobalSearchResponse> search(@RequestParam(required = false) String q) {
        String safeQuery = q == null ? "" : q.strip();
        if (safeQuery.length() > 200) {
            safeQuery = safeQuery.substring(0, 200);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(searchService.search(safeQuery));
    }
}
