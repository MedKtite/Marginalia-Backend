package com.readsake.dev.dto.uploads;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownloadUrlResponse {

    private String downloadUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime expiresAt;

    private String format;

    public DownloadUrlResponse(String downloadUrl, OffsetDateTime expiresAt) {
        this(downloadUrl, expiresAt, null);
    }

    public DownloadUrlResponse(String downloadUrl, OffsetDateTime expiresAt, String format) {
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
        this.format = format;
    }

    public String getDownloadUrl() { return downloadUrl; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public String getFormat() { return format; }
}
