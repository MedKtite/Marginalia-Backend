package com.marginalia.dev.dto.ingestion;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
public record IngestionJobResponse(UUID id, UUID bookId, String jobType, String sourceKey, String status, int attempts, Map<String, Object> result, String errorMessage, OffsetDateTime createdAt, OffsetDateTime startedAt, OffsetDateTime finishedAt) {}
