package com.marginalia.dev.dto.note;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

/**
 * PATCH semantics — every field optional; only non-null fields are applied.
 * Send {"bodyMd": "..."} to edit, {"isSaved": true|false} to (un)bookmark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteUpdateRequest {

    @Size(max = 10000)
    private String bodyMd;

    private Boolean isSaved;

    public String getBodyMd() { return bodyMd; }
    public void setBodyMd(String bodyMd) { this.bodyMd = bodyMd; }

    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
}
