package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Highlight;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HighlightDao {

    Optional<Highlight> findByIdAndUser(UUID id, UUID userId);

    List<Highlight> findByBook(UUID bookId, UUID userId);

    List<Highlight> findRecentForUser(UUID userId, int limit);

    Highlight insert(Highlight highlight);

    Optional<Highlight> patchColorTag(UUID id, UUID userId, String colorTag);

    boolean delete(UUID id, UUID userId);
}
