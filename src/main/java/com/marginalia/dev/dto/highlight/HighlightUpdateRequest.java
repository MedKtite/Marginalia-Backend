package com.marginalia.dev.dto.highlight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;

/**
 * PATCH semantics — every field optional; only non-null fields are applied.
 * Send {"colorTag": "..."} to re-color, {"isSaved": true|false} to (un)bookmark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HighlightUpdateRequest {

    @Pattern(regexp = "curious|resonant|beautiful|reference|urgent|disagree|revisit")
    private String colorTag;

    private Boolean isSaved;

    public String getColorTag() { return colorTag; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }

    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
}
