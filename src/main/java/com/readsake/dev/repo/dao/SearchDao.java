package com.readsake.dev.repo.dao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only DAO that powers the /search endpoint.
 * Returns raw row maps because hits are mixed-type.
 */
public interface SearchDao {

    List<Map<String, Object>> searchBooks(UUID userId, String query, int limit);

    List<Map<String, Object>> searchHighlights(UUID userId, String query, int limit);

    List<Map<String, Object>> searchNotes(UUID userId, String query, int limit);
}
