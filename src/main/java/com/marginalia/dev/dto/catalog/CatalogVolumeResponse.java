package com.marginalia.dev.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single Google Books search result, trimmed to what the client needs to
 * preview and add a book. Not persisted — the client adds via POST /me/books.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogVolumeResponse {

    private String googleId;
    private String title;
    private String author;
    private String publisher;
    private Integer publishedYear;
    private String isbn13;
    private Integer pageCount;
    private String thumbnailUrl;
    private String description;

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

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
