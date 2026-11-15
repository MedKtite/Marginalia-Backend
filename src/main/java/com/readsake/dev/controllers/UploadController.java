package com.readsake.dev.controllers;

import com.readsake.dev.dto.uploads.PresignUploadRequest;
import com.readsake.dev.dto.uploads.PresignUploadResponse;
import com.readsake.dev.exceptions.BadRequestException;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.FileStorageService;
import com.readsake.dev.services.FileStorageService.PresignedUpload;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Direct-to-S3 upload flow:
 *
 *   1) Client → POST /me/uploads/presign  with {format, contentType, contentLength}
 *   2) Server returns {fileKey, uploadUrl, expiresAt}
 *   3) Client PUTs the file body to uploadUrl
 *   4) Client → POST /me/books  with metadata + fileKey
 *      BookService HEADs the S3 object to confirm it landed, then persists the book.
 *
 * Backend never receives the file bytes — uploads scale independently of app servers.
 */
@RestController
@RequestMapping("/me/uploads")
public class UploadController {

    /** ContentType the client claims must match one of these per declared format. */
    private static final Map<String, String> EXPECTED_CONTENT_TYPE = Map.of(
            "epub", "application/epub+zip",
            "pdf",  "application/pdf",
            "m4b",  "audio/mp4",
            "mp3",  "audio/mpeg"
    );

    /** Autowired only if {@link com.readsake.dev.config.S3Config} created the beans. */
    @Autowired(required = false)
    private FileStorageService storage;

    @Value("${s3.max-upload-bytes:1073741824}")
    private long maxUploadBytes;

    @PostMapping("/presign")
    public ResponseEntity<PresignUploadResponse> presign(
            @AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody PresignUploadRequest req) {

        if (storage == null) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
        }

        // 1) Size cap
        if (req.getContentLength() > maxUploadBytes) {
            throw new BadRequestException(
                    "File too large: limit is " + maxUploadBytes + " bytes "
                    + "(" + (maxUploadBytes / (1024 * 1024)) + " MiB).");
        }

        // 2) ContentType ↔ format consistency
        String expected = EXPECTED_CONTENT_TYPE.get(req.getFormat());
        if (expected == null) {
            throw new BadRequestException("Unsupported format: " + req.getFormat());
        }
        if (!expected.equalsIgnoreCase(req.getContentType())) {
            throw new BadRequestException(
                    "Content type for format '" + req.getFormat() + "' must be '" + expected
                    + "', got '" + req.getContentType() + "'.");
        }

        // 3) Build per-user key — naturally isolates tenants in S3.
        String fileKey = "users/" + me.getUserId() + "/uploads/"
                + UUID.randomUUID() + "." + req.getFormat();

        PresignedUpload upload = storage.presignUpload(
                fileKey, req.getContentType(), req.getContentLength());

        return ResponseEntity.ok(new PresignUploadResponse(
                fileKey, upload.url(), upload.method(), upload.expiresAt()));
    }
}
