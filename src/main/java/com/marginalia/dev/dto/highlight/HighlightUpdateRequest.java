package com.marginalia.dev.dto.highlight;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HighlightUpdateRequest {

    /** Currently the only mutable field — re-color the highlight. */
    @NotBlank
    @Pattern(regexp = "curious|resonant|beautiful|reference|urgent|disagree|revisit")
    private String colorTag;

    public String getColorTag() { return colorTag; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }
}
