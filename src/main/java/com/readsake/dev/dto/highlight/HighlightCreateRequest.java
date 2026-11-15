package com.readsake.dev.dto.highlight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HighlightCreateRequest {

    @NotNull
    private UUID bookId;

    /** "§47", "Chapter 4", or null for audio bookmarks. */
    private String textChapterRef;

    /** Character offset in the chapter where the highlight begins. */
    private Integer textStartOffset;

    private Integer textEndOffset;

    /** The highlighted passage. Null for pure audio bookmarks. */
    private String passageText;

    /** Audio timestamp in seconds. Null for text highlights. */
    private BigDecimal audioStartSec;

    private BigDecimal audioEndSec;

    @NotBlank
    @Pattern(regexp = "curious|resonant|beautiful|reference|urgent|disagree|revisit")
    private String colorTag;

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
}
