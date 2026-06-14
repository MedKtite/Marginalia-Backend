package com.marginalia.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.books.
 * Covers uploaded files (epub/pdf/m4b/mp3) and manually logged physical books.
 * The cursor column is JSONB — service layer parses to/from a chapter/offset object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {

    private UUID id;

    private UUID userId;

    // ── Metadata ──
    private String title;

    private String author;

    private String narrator;

    private String publisher;

    private Integer publishedYear;

    private String isbn13;

    private Integer pageCount;

    private Integer durationSec;

    private String language;

    // ── Format + storage ──
    private String format;            // 'epub' | 'pdf' | 'm4b' | 'mp3' | 'physical'

    private String fileKey;

    private Long fileSizeBytes;

    private String coverUrl;

    private String coverDominantColor;

    // ── Reading state ──
    private String status;            // 'reading' | 'listening' | 'finished' | 'archived'

    private BigDecimal progressPct;   // 0.00–100.00

    private String cursor;            // JSONB as raw string

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastOpenedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime finishedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

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

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

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
