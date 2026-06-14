package com.marginalia.dev.dto.preferences;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * PATCH semantics — null fields are not touched.
 * homeSections is accepted as a raw JSON object and re-serialized to a string
 * before being passed to the DAO (the column is JSONB).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferencesUpdateRequest {

    @Pattern(regexp = "light|dark|system")
    private String theme;

    @Pattern(regexp = "spacious|compact")
    private String density;

    @Pattern(regexp = "morning|afternoon|evening|night")
    private String greetingOverride;

    @JsonRawValue
    private String homeSections;

    @Min(14) @Max(22)
    private Integer readingFontSize;

    @Min(100) @Max(220)
    private Integer readingLineHeight;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getDensity() { return density; }
    public void setDensity(String density) { this.density = density; }

    public String getGreetingOverride() { return greetingOverride; }
    public void setGreetingOverride(String greetingOverride) { this.greetingOverride = greetingOverride; }

    public String getHomeSections() { return homeSections; }

    /** Accept an arbitrary JSON object and stash the raw JSON text. */
    @JsonSetter("homeSections")
    public void setHomeSections(JsonNode node) {
        this.homeSections = node == null || node.isNull() ? null : node.toString();
    }

    public Integer getReadingFontSize() { return readingFontSize; }
    public void setReadingFontSize(Integer readingFontSize) { this.readingFontSize = readingFontSize; }

    public Integer getReadingLineHeight() { return readingLineHeight; }
    public void setReadingLineHeight(Integer readingLineHeight) { this.readingLineHeight = readingLineHeight; }
}
