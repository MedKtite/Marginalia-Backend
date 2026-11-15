package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Highlight;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HighlightDao {

    Optional<Highlight> findByIdAndUser(UUID id, UUID userId);

    List<Highlight> findByBook(UUID bookId, UUID userId);

    List<Highlight> findRecentForUser(UUID userId, int limit);

    /** Highlights bookmarked into the user's Saved collection, newest first. */
    List<Highlight> findSavedForUser(UUID userId);

    /** Highlights carrying this tag (via highlight_tags), newest first. */
    List<Highlight> findByTag(UUID tagId, UUID userId);

    Highlight insert(Highlight highlight);

    Optional<Highlight> patchColorTag(UUID id, UUID userId, String colorTag);

    Optional<Highlight> patchSaved(UUID id, UUID userId, boolean isSaved);

    boolean delete(UUID id, UUID userId);
}
