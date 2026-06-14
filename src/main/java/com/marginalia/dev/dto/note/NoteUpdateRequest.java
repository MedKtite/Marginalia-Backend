package com.marginalia.dev.dto.note;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteUpdateRequest {

    @NotBlank
    @Size(max = 10000)
    private String bodyMd;

    public String getBodyMd() { return bodyMd; }
    public void setBodyMd(String bodyMd) { this.bodyMd = bodyMd; }
}
