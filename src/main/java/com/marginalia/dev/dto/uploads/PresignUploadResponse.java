package com.marginalia.dev.dto.uploads;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresignUploadResponse {

    /** The opaque storage key — the client passes this back when creating the Book row. */
    private String fileKey;

    /** Pre-signed URL the client PUTs the file to (one shot, time-limited). */
    private String uploadUrl;

    /** HTTP method to use against uploadUrl. Always PUT today. */
    private String method;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime expiresAt;

    public PresignUploadResponse(String fileKey, String uploadUrl, String method, OffsetDateTime expiresAt) {
        this.fileKey = fileKey;
        this.uploadUrl = uploadUrl;
        this.method = method;
        this.expiresAt = expiresAt;
    }

    public String getFileKey() { return fileKey; }
    public String getUploadUrl() { return uploadUrl; }
    public String getMethod() { return method; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
