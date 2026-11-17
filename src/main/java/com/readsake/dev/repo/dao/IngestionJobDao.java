package com.readsake.dev.repo.dao;
import com.readsake.dev.entities.IngestionJob;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
public interface IngestionJobDao {
    IngestionJob insert(UUID bookId, String jobType, String sourceKey, Map<String, Object> payload);
    boolean insertIfAbsent(UUID bookId, String jobType, String sourceKey, Map<String, Object> payload);
    Optional<IngestionJob> latestForSource(String jobType, String sourceKey);
    boolean gutenbergUnavailable();
    Optional<IngestionJob> findByIdAndBook(UUID id, UUID bookId);
}
