package com.marginalia.dev.dto.highlight;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.marginalia.dev.entities.Highlight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HighlightResponse {

    private UUID id;
    private UUID bookId;
    private String textChapterRef;
    private Integer textStartOffset;
    private Integer textEndOffset;
    private String passageText;
    private BigDecimal audioStartSec;
    private BigDecimal audioEndSec;
    private String colorTag;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    public static HighlightResponse from(Highlight h) {
        HighlightResponse r = new HighlightResponse();
        r.id              = h.getId();
        r.bookId          = h.getBookId();
        r.textChapterRef  = h.getTextChapterRef();
        r.textStartOffset = h.getTextStartOffset();
        r.textEndOffset   = h.getTextEndOffset();
        r.passageText     = h.getPassageText();
        r.audioStartSec   = h.getAudioStartSec();
        r.audioEndSec     = h.getAudioEndSec();
        r.colorTag        = h.getColorTag();
        r.createdAt       = h.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
}
