package com.readsake.dev.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /me/catalog/requests — "Request this book" on a
 * metadata-only catalog title. Pure demand signal: rows feed future
 * licensing decisions, nothing is created in the user's library.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookRequestCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 500)
    private String author;

    @Size(max = 20)
    private String isbn13;

    @Size(max = 40)
    private String googleId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
}
