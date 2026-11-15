package com.readsake.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.user_preferences.
 * The home_sections column is JSONB — kept as raw JSON string here; service layer
 * parses to/from Map&lt;String,Boolean&gt; as needed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferences {

    private UUID userId;

    private String theme;        // 'light' | 'dark' | 'system'

    private String density;      // 'spacious' | 'compact'

    private String greetingOverride; // 'morning' | 'afternoon' | 'evening' | 'night' | null

    private String homeSections; // raw JSON

    private Integer readingFontSize;

    private Integer readingLineHeight;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

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
