package com.marginalia.dev.repo.dao;
import com.marginalia.dev.entities.IngestionJob;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
public interface IngestionJobDao { IngestionJob insert(UUID bookId,String jobType,String sourceKey,Map<String,Object> payload); Optional<IngestionJob> findByIdAndBook(UUID id,UUID bookId); }
