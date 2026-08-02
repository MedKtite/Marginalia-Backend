package com.marginalia.dev.dto.book;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.marginalia.dev.entities.Book;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookResponse {

    private UUID id;
    private String title;
    private String author;
    private String narrator;
    private String publisher;
    private Integer publishedYear;
    private String isbn13;
    private String googleId;
    private Integer gutenbergId;
    private Integer pageCount;
    private Integer durationSec;
    private String language;
    private String format;
    private String coverUrl;
    private String coverDominantColor;
    private String processingStatus;
    private String processingError;
    private String readingFileKey;
    private String processingVersion;
    private String description;
    private String status;
    private BigDecimal progressPct;
    private String cursor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastOpenedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime finishedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public static BookResponse from(Book b) {
        BookResponse r = new BookResponse();
        r.id                  = b.getId();
        r.title               = b.getTitle();
        r.author              = b.getAuthor();
        r.narrator            = b.getNarrator();
        r.publisher           = b.getPublisher();
        r.publishedYear       = b.getPublishedYear();
        r.isbn13              = b.getIsbn13();
        r.googleId            = b.getGoogleId();
        r.gutenbergId         = b.getGutenbergId();
        r.pageCount           = b.getPageCount();
        r.durationSec         = b.getDurationSec();
        r.language            = b.getLanguage();
        r.format              = b.getFormat();
        r.coverUrl            = b.getCoverUrl();
        r.coverDominantColor  = b.getCoverDominantColor();
        r.processingStatus    = b.getProcessingStatus();
        r.processingError     = b.getProcessingError();
        r.readingFileKey      = b.getReadingFileKey();
        r.processingVersion   = b.getProcessingVersion();
        r.description         = b.getDescription();
        r.status              = b.getStatus();
        r.progressPct         = b.getProgressPct();
        r.cursor              = b.getCursor();
        r.lastOpenedAt        = b.getLastOpenedAt();
        r.finishedAt          = b.getFinishedAt();
        r.createdAt           = b.getCreatedAt();
        r.updatedAt           = b.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getNarrator() { return narrator; }
    public void setNarrator(String narrator) { this.narrator = narrator; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }

    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public Integer getGutenbergId() { return gutenbergId; }
    public void setGutenbergId(Integer gutenbergId) { this.gutenbergId = gutenbergId; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getCoverDominantColor() { return coverDominantColor; }
    public void setCoverDominantColor(String coverDominantColor) { this.coverDominantColor = coverDominantColor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getProgressPct() { return progressPct; }
    public void setProgressPct(BigDecimal progressPct) { this.progressPct = progressPct; }

    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }

    public OffsetDateTime getLastOpenedAt() { return lastOpenedAt; }
    public void setLastOpenedAt(OffsetDateTime lastOpenedAt) { this.lastOpenedAt = lastOpenedAt; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
