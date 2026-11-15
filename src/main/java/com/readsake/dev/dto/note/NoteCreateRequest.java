package com.readsake.dev.dto.note;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteCreateRequest {

    @NotNull
    private UUID bookId;

    /** Optional — when set, the note is attached to a specific highlight. */
    private UUID highlightId;

    @NotBlank
    @Size(max = 10000)
    private String bodyMd;

    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }

    public UUID getHighlightId() { return highlightId; }
    public void setHighlightId(UUID highlightId) { this.highlightId = highlightId; }

    public String getBodyMd() { return bodyMd; }
    public void setBodyMd(String bodyMd) { this.bodyMd = bodyMd; }
}
