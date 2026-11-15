package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagDao {

    /** All system tags, in insertion order. */
    List<Tag> findSystemTags();

    /** The system tag with this name (case-insensitive), if seeded. */
    Optional<Tag> findSystemByName(String name);

    /** Both system and the user's custom tags. */
    List<Tag> findVisibleForUser(UUID userId);

    /** Returns the tag if it's a system tag or owned by the user. */
    Optional<Tag> findVisibleByIdForUser(UUID id, UUID userId);

    /** Returns the tag if it's user-owned (not system). */
    Optional<Tag> findOwnedByUser(UUID id, UUID userId);

    Tag insertCustom(UUID userId, String name, String colorHex);

    /** PATCH semantics — null fields stay. */
    Optional<Tag> updateCustom(UUID id, UUID userId, String name, String colorHex);

    boolean deleteCustom(UUID id, UUID userId);

    /**
     * Attach a tag to a highlight. Caller must have already verified that
     * the highlight belongs to the user and the tag is visible to them.
     * Idempotent — repeat attaches succeed silently.
     */
    void attachToHighlight(UUID highlightId, UUID tagId);

    boolean detachFromHighlight(UUID highlightId, UUID tagId);

    /** Tags currently attached to this highlight, in stable insertion order. */
    List<Tag> findByHighlight(UUID highlightId);

    /** highlight count per tag for this user's highlights (absent = zero). */
    java.util.Map<UUID, Integer> countHighlightsByTag(UUID userId);
}
