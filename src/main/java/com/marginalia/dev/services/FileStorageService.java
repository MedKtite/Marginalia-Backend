package com.marginalia.dev.services;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface FileStorageService {

    /**
     * Issue a short-lived PUT URL the client uploads directly to. Backend never
     * sees the bytes. Caller is responsible for validating format/size limits
     * before calling.
     */
    PresignedUpload presignUpload(String fileKey, String contentType, long contentLength);

    /** Short-lived GET URL the client uses to download or stream the file. */
    PresignedDownload presignDownload(String fileKey);

    /** Verify the object actually landed; returns its size if so. */
    Optional<Long> headObject(String fileKey);

    /** Read an object for server-side derivation (for example PDF → EPUB). */
    byte[] readObject(String fileKey);

    /** Store a backend-generated derivative. */
    void putObject(String fileKey, byte[] bytes, String contentType);

    /** Best-effort delete — does not throw if the object is already gone. */
    void deleteObject(String fileKey);

    record PresignedUpload(String url, OffsetDateTime expiresAt, String method) {}

    record PresignedDownload(String url, OffsetDateTime expiresAt) {}
}
