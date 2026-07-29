package com.marginalia.dev.repo.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marginalia.dev.entities.IngestionJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IngestionJobDaoImpl implements IngestionJobDao {
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String COLUMNS = "id, book_id, job_type, source_key, status, attempts, result, error_message, created_at, started_at, finished_at";

    private IngestionJob map(Map<String, Object> row) {
        IngestionJob j = new IngestionJob();
        j.setId((UUID) row.get("id")); j.setBookId((UUID) row.get("book_id"));
        j.setJobType((String) row.get("job_type")); j.setSourceKey((String) row.get("source_key"));
        j.setStatus((String) row.get("status")); j.setAttempts(((Number) row.get("attempts")).intValue());
        try { j.setResult(row.get("result") == null ? null : objectMapper.readValue(row.get("result").toString(), Map.class)); }
        catch (Exception ignored) { j.setResult(Map.of()); }
        j.setErrorMessage((String) row.get("error_message"));
        j.setCreatedAt(time(row.get("created_at"))); j.setStartedAt(time(row.get("started_at"))); j.setFinishedAt(time(row.get("finished_at")));
        return j;
    }

    private static java.time.OffsetDateTime time(Object value) {
        return value == null ? null : ((Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
    }

    @Override public IngestionJob insert(UUID bookId, String jobType, String sourceKey, Map<String, Object> payload) {
        String sql = "INSERT INTO ingestion_jobs (book_id, job_type, source_key, payload) VALUES (:bookId, :jobType, :sourceKey, CAST(:payload AS jsonb)) RETURNING " + COLUMNS;
        return jdbc.queryForObject(sql, new MapSqlParameterSource().addValue("bookId", bookId).addValue("jobType", jobType).addValue("sourceKey", sourceKey).addValue("payload", write(payload)),
                (rs, n) -> map(new ColumnMapRowMapper().mapRow(rs, n)));
    }

    @Override public Optional<IngestionJob> findByIdAndBook(UUID id, UUID bookId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ingestion_jobs WHERE id = :id AND book_id = :bookId",
                new MapSqlParameterSource().addValue("id", id).addValue("bookId", bookId),
                (rs, n) -> map(new ColumnMapRowMapper().mapRow(rs, n))).stream().findFirst();
    }

    private String write(Map<String, Object> value) {
        try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid job payload", e); }
    }
}
