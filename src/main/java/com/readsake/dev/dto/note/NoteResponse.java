package com.readsake.dev.dto.note;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.readsake.dev.entities.Note;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteResponse {

    private UUID id;
    private UUID bookId;
    private UUID highlightId;
    private String bodyMd;
    private Boolean isSaved;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public static NoteResponse from(Note n) {
        NoteResponse r = new NoteResponse();
        r.id          = n.getId();
        r.bookId      = n.getBookId();
        r.highlightId = n.getHighlightId();
        r.bodyMd      = n.getBodyMd();
        r.isSaved     = n.getIsSaved();
        r.createdAt   = n.getCreatedAt();
        r.updatedAt   = n.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }
    public UUID getHighlightId() { return highlightId; }
    public void setHighlightId(UUID highlightId) { this.highlightId = highlightId; }
    public String getBodyMd() { return bodyMd; }
    public void setBodyMd(String bodyMd) { this.bodyMd = bodyMd; }
    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
