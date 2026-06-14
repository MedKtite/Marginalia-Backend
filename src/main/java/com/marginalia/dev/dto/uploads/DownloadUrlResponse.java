package com.marginalia.dev.dto.uploads;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownloadUrlResponse {

    private String downloadUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime expiresAt;

    public DownloadUrlResponse(String downloadUrl, OffsetDateTime expiresAt) {
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
    }

    public String getDownloadUrl() { return downloadUrl; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
