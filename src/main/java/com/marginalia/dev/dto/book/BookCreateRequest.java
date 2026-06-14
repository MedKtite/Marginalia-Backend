package com.marginalia.dev.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BookCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 300)
    private String author;

    @Size(max = 300)
    private String narrator;

    @Size(max = 300)
    private String publisher;

    private Integer publishedYear;

    /**
     * S3 object key returned by POST /me/uploads/presign. Required for non-physical
     * formats when this book is being created from an upload. Service verifies the
     * object exists in storage before persisting.
     */
    @Size(max = 500)
    private String fileKey;

    @Size(min = 13, max = 13)
    private String isbn13;

    private Integer pageCount;

    private Integer durationSec;

    @Size(min = 2, max = 2)
    private String language;

    /** One of: epub | pdf | m4b | mp3 | physical */
    @NotBlank
    @Pattern(regexp = "epub|pdf|m4b|mp3|physical")
    private String format;

    private String coverUrl;

    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "must be a #RRGGBB hex color")
    private String coverDominantColor;

    /** Optional override; if absent, service picks reading/listening from format. */
    @Pattern(regexp = "reading|listening|finished|archived")
    private String status;

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

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

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

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getCoverDominantColor() { return coverDominantColor; }
    public void setCoverDominantColor(String coverDominantColor) { this.coverDominantColor = coverDominantColor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
