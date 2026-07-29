package com.marginalia.dev.dto.ingestion;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
public record IngestionJobCreateRequest(@NotBlank String jobType, String sourceKey, Map<String, Object> payload) {}
