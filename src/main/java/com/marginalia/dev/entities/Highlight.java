package com.marginalia.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.highlights.
 * One table for both text highlights and audio bookmarks. The search_tsv column
 * is maintained server-side by a trigger and is intentionally not modeled here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Highlight {

    private UUID id;

    private UUID userId;

    private UUID bookId;

    // ── Text location (epub/pdf) ──
    private String textChapterRef;

    private Integer textStartOffset;

    private Integer textEndOffset;

    private String passageText;

    // ── Audio location (m4b/mp3) ──
    private BigDecimal audioStartSec;

    private BigDecimal audioEndSec;

    // ── Styling ──
    private String colorTag; // one of: curious | resonant | beautiful | reference | urgent | disagree | revisit

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }

    public String getTextChapterRef() { return textChapterRef; }
    public void setTextChapterRef(String textChapterRef) { this.textChapterRef = textChapterRef; }

    public Integer getTextStartOffset() { return textStartOffset; }
    public void setTextStartOffset(Integer textStartOffset) { this.textStartOffset = textStartOffset; }

    public Integer getTextEndOffset() { return textEndOffset; }
    public void setTextEndOffset(Integer textEndOffset) { this.textEndOffset = textEndOffset; }

    public String getPassageText() { return passageText; }
    public void setPassageText(String passageText) { this.passageText = passageText; }

    public BigDecimal getAudioStartSec() { return audioStartSec; }
    public void setAudioStartSec(BigDecimal audioStartSec) { this.audioStartSec = audioStartSec; }

    public BigDecimal getAudioEndSec() { return audioEndSec; }
    public void setAudioEndSec(BigDecimal audioEndSec) { this.audioEndSec = audioEndSec; }

    public String getColorTag() { return colorTag; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
