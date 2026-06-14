package com.marginalia.dev.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Mirrors row in public.highlight_tags — many-to-many join between highlights and tags.
 * Composite primary key: (highlight_id, tag_id).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HighlightTag {

    private UUID highlightId;

    private UUID tagId;

    public UUID getHighlightId() { return highlightId; }
    public void setHighlightId(UUID highlightId) { this.highlightId = highlightId; }

    public UUID getTagId() { return tagId; }
    public void setTagId(UUID tagId) { this.tagId = tagId; }
}
