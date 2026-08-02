package com.marginalia.dev.repo.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Access to the gutenberg_catalog table — the nightly-synced Project Gutenberg
 * index that backs Discovery search's "readable in-app" layer.
 */
public interface GutenbergCatalogDao {

    /** Row count — 0 means the index has never been synced. */
    long count();

    /**
     * Upsert a batch of catalog rows. Each row:
     * {gutenbergId, title, authors, language, subjects, titleNorm}.
     */
    void upsertBatch(List<Object[]> rows);

    /**
     * Full-text search on title + authors. Rows: gutenberg_id, title, authors,
     * language. English titles rank first.
     */
    List<Map<String, Object>> search(String query, int limit);

    /** Rows whose normalized title equals [titleNorm] (small candidate set). */
    List<Map<String, Object>> findByTitleNorm(String titleNorm);

    List<Map<String, Object>> trending(int limit);

    List<Map<String, Object>> topAuthors(int limit);

    Optional<String> findReaderFileKey(int gutenbergId);

    Optional<String> findReaderFileKeyByTitle(String title);

    Optional<Integer> findGutenbergIdByTitle(String title);
}
