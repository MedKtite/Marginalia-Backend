package com.readsake.dev.controllers;

import com.readsake.dev.dto.search.SearchResponse;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * @param q     query string (the Search tab's input)
     * @param scope all | books | highlights | notes
     * @param limit max hits per type (1..50, default 20)
     */
    @GetMapping
    public SearchResponse search(@AuthenticationPrincipal CustomUserDetails me,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(defaultValue = "all") String scope,
                                 @RequestParam(defaultValue = "20") int limit) {
        return searchService.search(me.getUserId(), q, scope, limit);
    }
}
