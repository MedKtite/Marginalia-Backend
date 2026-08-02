package com.marginalia.dev.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A precomputed top-author row from the nightly Gutenberg snapshot. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopAuthorResponse {

    private String name;
    private Integer rank;
    private Long downloadCount;
    private Integer bookCount;
    private String imageUrl;
    private String tagline;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }

    public Integer getBookCount() { return bookCount; }
    public void setBookCount(Integer bookCount) { this.bookCount = bookCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
}
