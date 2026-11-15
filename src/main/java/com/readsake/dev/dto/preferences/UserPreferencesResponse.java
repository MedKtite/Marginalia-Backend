package com.readsake.dev.dto.preferences;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.readsake.dev.entities.UserPreferences;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferencesResponse {

    private UUID userId;
    private String theme;
    private String density;
    private String greetingOverride;

    /** Emit homeSections raw — it's stored as JSON in DB so it round-trips as a real object. */
    @JsonRawValue
    private String homeSections;

    private Integer readingFontSize;
    private Integer readingLineHeight;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public static UserPreferencesResponse from(UserPreferences p) {
        UserPreferencesResponse r = new UserPreferencesResponse();
        r.userId            = p.getUserId();
        r.theme             = p.getTheme();
        r.density           = p.getDensity();
        r.greetingOverride  = p.getGreetingOverride();
        r.homeSections      = p.getHomeSections();
        r.readingFontSize   = p.getReadingFontSize();
        r.readingLineHeight = p.getReadingLineHeight();
        r.updatedAt         = p.getUpdatedAt();
        return r;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getDensity() { return density; }
    public void setDensity(String density) { this.density = density; }

    public String getGreetingOverride() { return greetingOverride; }
    public void setGreetingOverride(String greetingOverride) { this.greetingOverride = greetingOverride; }

    public String getHomeSections() { return homeSections; }
    public void setHomeSections(String homeSections) { this.homeSections = homeSections; }

    public Integer getReadingFontSize() { return readingFontSize; }
    public void setReadingFontSize(Integer readingFontSize) { this.readingFontSize = readingFontSize; }

    public Integer getReadingLineHeight() { return readingLineHeight; }
    public void setReadingLineHeight(Integer readingLineHeight) { this.readingLineHeight = readingLineHeight; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
