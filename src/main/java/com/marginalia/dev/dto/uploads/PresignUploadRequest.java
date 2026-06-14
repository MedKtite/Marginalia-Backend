package com.marginalia.dev.dto.uploads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PresignUploadRequest {

    /** One of: epub | pdf | m4b | mp3 — physical books don't upload. */
    @NotBlank
    @Pattern(regexp = "epub|pdf|m4b|mp3")
    private String format;

    /** MIME type the client will set on the PUT (must match the presign). */
    @NotBlank
    private String contentType;

    /** Exact byte length the client will upload. Required by S3 signature. */
    @Min(1)
    private long contentLength;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getContentLength() { return contentLength; }
    public void setContentLength(long contentLength) { this.contentLength = contentLength; }
}
