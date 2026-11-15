package com.readsake.dev.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 wiring. The same beans serve real AWS S3 and the local MinIO container:
 *
 *   • {@code s3.endpoint} blank      → real AWS (uses regional endpoint, virtual-host style)
 *   • {@code s3.endpoint} set        → MinIO/R2 (forces path-style addressing)
 *
 * In dev the bucket is auto-created on startup if missing.
 *
 * Disabled (no beans created) when {@code s3.bucket} is blank — UploadController
 * then returns 503 with a clear message instead of erroring at request time.
 */
@Configuration
@ConditionalOnProperty(name = "s3.bucket")
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${s3.endpoint:}")
    private String endpoint;

    /** Endpoint reachable by the backend process for HEAD/GET/DELETE calls. */
    @Value("${s3.internal-endpoint:${s3.endpoint:}}")
    private String internalEndpoint;

    @Value("${s3.region:us-east-1}")
    private String region;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.access-key-id:}")
    private String accessKeyId;

    @Value("${s3.secret-access-key:}")
    private String secretAccessKey;

    @Value("${s3.force-path-style:false}")
    private boolean forcePathStyle;

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .endpointOverride(endpointUri(internalEndpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(forcePathStyle)
                        .build())
                .build();

        ensureBucket(client);
        return client;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .endpointOverride(endpointUri(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(forcePathStyle)
                        .build())
                .build();
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials() {
        // If explicit creds are supplied (MinIO, dev) use them; otherwise fall
        // back to the AWS default chain (IAM role, env vars, profile, etc).
        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        return DefaultCredentialsProvider.create();
    }

    private URI endpointUri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value);
    }

    /** Idempotent — creates the bucket once on first startup against a fresh MinIO. */
    private void ensureBucket(S3Client client) {
        try {
            client.headBucket(b -> b.bucket(bucket));
            log.info("S3 bucket '{}' present (endpoint={})", bucket,
                    endpoint.isBlank() ? "aws-default" : endpoint);
        } catch (NoSuchBucketException e) {
            log.info("Creating S3 bucket '{}' on first startup", bucket);
            client.createBucket(b -> b.bucket(bucket));
        } catch (Exception e) {
            log.warn("S3 bucket check failed (endpoint={}, bucket={}): {} — uploads will fail until resolved",
                    endpoint.isBlank() ? "aws-default" : endpoint, bucket, e.getMessage());
        }
    }
}
