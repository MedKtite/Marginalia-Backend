package com.readsake.dev.services;

import com.readsake.dev.dto.search.SearchResponse;

import java.util.UUID;

public interface SearchService {

    /**
     * @param scope "all" | "books" | "highlights" | "notes"
     * @param limit maximum hits PER type (so an all-scope query can return 3 * limit)
     */
    SearchResponse search(UUID userId, String query, String scope, int limit);
}
