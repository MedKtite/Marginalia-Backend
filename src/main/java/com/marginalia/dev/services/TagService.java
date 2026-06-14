package com.marginalia.dev.services;

import com.marginalia.dev.dto.tag.TagCreateRequest;
import com.marginalia.dev.dto.tag.TagUpdateRequest;
import com.marginalia.dev.entities.Tag;

import java.util.List;
import java.util.UUID;

public interface TagService {

    List<Tag> listForUser(UUID userId);

    Tag createCustom(TagCreateRequest req, UUID userId);

    Tag updateCustom(UUID id, TagUpdateRequest req, UUID userId);

    void deleteCustom(UUID id, UUID userId);

    /** Verifies highlight ownership + tag visibility, then attaches. */
    void attachTag(UUID highlightId, UUID tagId, UUID userId);

    void detachTag(UUID highlightId, UUID tagId, UUID userId);

    /** Tags attached to a highlight the caller owns. */
    List<Tag> listForHighlight(UUID highlightId, UUID userId);
}
