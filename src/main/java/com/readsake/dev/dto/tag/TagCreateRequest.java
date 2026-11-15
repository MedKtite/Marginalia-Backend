package com.readsake.dev.dto.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TagCreateRequest {

    @NotBlank
    @Size(min = 1, max = 40)
    private String name;

    @NotBlank
    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "must be a #RRGGBB hex color")
    private String colorHex;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
