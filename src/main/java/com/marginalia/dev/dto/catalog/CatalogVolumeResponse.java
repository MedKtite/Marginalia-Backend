package com.marginalia.dev.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single catalog search result — from the synced Gutenberg index or from
 * Google Books — trimmed to what the client needs to preview and add a book.
 * Not persisted — the client adds via POST /me/books.
 *
 * [source] is GUTENBERG or GOOGLE; [contentAvailability] is FULL (readable /
 * listenable in-app via the public-domain EPUB) or METADATA_ONLY.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogVolumeResponse {

    public static final String SOURCE_GUTENBERG = "GUTENBERG";
    public static final String SOURCE_GOOGLE = "GOOGLE";
    public static final String AVAILABILITY_FULL = "FULL";
    public static final String AVAILABILITY_METADATA_ONLY = "METADATA_ONLY";

    private String source;
    private String contentAvailability;
    private Integer gutenbergId;
    private String googleId;
    private String title;
    private String author;
    private String publisher;
    private Integer publishedYear;
    private String isbn13;
    private Integer pageCount;
    private String language;
    private String thumbnailUrl;
    private String description;

    /** Google Books community rating, 1.0–5.0. Null when Google has none. */
    private Double averageRating;

    /** Number of Google ratings behind [averageRating] — the "readers" KPI. */
    private Integer ratingsCount;

    /** Google Books web preview/sample URL. Null when no preview is offered. */
    private String previewUrl;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getContentAvailability() { return contentAvailability; }
    public void setContentAvailability(String contentAvailability) { this.contentAvailability = contentAvailability; }

    public Integer getGutenbergId() { return gutenbergId; }
    public void setGutenbergId(Integer gutenbergId) { this.gutenbergId = gutenbergId; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }

    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
}
