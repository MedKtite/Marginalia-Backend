package com.marginalia.dev.services;

import com.marginalia.dev.dto.highlight.HighlightCreateRequest;
import com.marginalia.dev.dto.highlight.HighlightUpdateRequest;
import com.marginalia.dev.entities.Highlight;

import java.util.List;
import java.util.UUID;

public interface HighlightService {

    Highlight create(HighlightCreateRequest req, UUID userId);

    Highlight getOne(UUID id, UUID userId);

    List<Highlight> listForBook(UUID bookId, UUID userId);

    List<Highlight> listRecent(UUID userId, int limit);

    Highlight updateColor(UUID id, HighlightUpdateRequest req, UUID userId);

    void delete(UUID id, UUID userId);
}
