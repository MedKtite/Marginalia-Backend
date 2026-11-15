package com.readsake.dev.services;

import com.readsake.dev.dto.highlight.HighlightCreateRequest;
import com.readsake.dev.dto.highlight.HighlightUpdateRequest;
import com.readsake.dev.entities.Highlight;

import java.util.List;
import java.util.UUID;

public interface HighlightService {

    Highlight create(HighlightCreateRequest req, UUID userId);

    Highlight getOne(UUID id, UUID userId);

    List<Highlight> listForBook(UUID bookId, UUID userId);

    List<Highlight> listRecent(UUID userId, int limit);

    List<Highlight> listSaved(UUID userId);

    List<Highlight> listByTag(UUID tagId, UUID userId);

    /** Partial update: colorTag and/or isSaved — only non-null fields apply. */
    Highlight update(UUID id, HighlightUpdateRequest req, UUID userId);

    void delete(UUID id, UUID userId);
}
