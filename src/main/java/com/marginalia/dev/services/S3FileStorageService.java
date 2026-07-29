package com.marginalia.dev.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * S3-backed implementation. Active when an {@link S3Client} bean exists
 * (i.e. {@code s3.bucket} is configured). When the bean is missing,
 * UploadController short-circuits to a 503 — so this service can assume
 * S3 is available.
 */
@Service
@ConditionalOnBean(S3Client.class)
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    @Autowired
    private S3Client s3;

    @Autowired
    private S3Presigner presigner;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.presign-expires-min:15}")
    private int presignExpiresMin;

    @Override
    public PresignedUpload presignUpload(String fileKey, String contentType, long contentLength) {
        Duration ttl = Duration.ofMinutes(presignExpiresMin);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(put)
                .build();

        String url = presigner.presignPutObject(presign).url().toString();
        return new PresignedUpload(url, expiresAt(ttl), "PUT");
    }

    @Override
    public PresignedDownload presignDownload(String fileKey) {
        // Short window for downloads — 5 minutes regardless of upload TTL.
        Duration ttl = Duration.ofMinutes(5);

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();

        String url = presigner.presignGetObject(presign).url().toString();
        return new PresignedDownload(url, expiresAt(ttl));
    }

    @Override
    public Optional<Long> headObject(String fileKey) {
        try {
            HeadObjectResponse resp = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build());
            return Optional.of(resp.contentLength());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("HEAD {} failed: {}", fileKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public byte[] readObject(String fileKey) {
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build()).asByteArray();
    }

    @Override
    public void putObject(String fileKey, byte[] bytes, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileKey)
                        .contentType(contentType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public void deleteObject(String fileKey) {
        try {
            s3.deleteObject(d -> d.bucket(bucket).key(fileKey));
        } catch (Exception e) {
            // Don't fail the calling transaction over storage cleanup.
            log.warn("Failed to delete S3 object {}: {}", fileKey, e.getMessage());
        }
    }

    private static OffsetDateTime expiresAt(Duration ttl) {
        return OffsetDateTime.now(ZoneOffset.UTC).plus(ttl);
    }
}
