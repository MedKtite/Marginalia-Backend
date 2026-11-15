package com.readsake.dev.repo.dao;

import java.util.List;
import java.util.UUID;

/** Per-user catalog search history — the signal behind "Recommended for you". */
public interface SearchQueryDao {

    void insert(UUID userId, String query);

    /** Most recent distinct queries, newest first. */
    List<String> recentDistinct(UUID userId, int limit);
}
