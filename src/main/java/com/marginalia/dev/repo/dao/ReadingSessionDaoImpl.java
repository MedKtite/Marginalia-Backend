package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.ReadingSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ReadingSessionDaoImpl implements ReadingSessionDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<ReadingSession> MAPPER =
            new BeanPropertyRowMapper<>(ReadingSession.class);

    private static final String COLUMNS = """
            id, user_id, book_id, started_at, ended_at, duration_sec, device_id,
            start_progress_pct, end_progress_pct
            """;

    @Override
    public ReadingSession insert(ReadingSession s) {
        String sql = """
                INSERT INTO reading_sessions (
                    user_id, book_id, started_at, ended_at, duration_sec,
                    device_id, start_progress_pct, end_progress_pct
                ) VALUES (
                    :userId, :bookId, :startedAt, :endedAt, :durationSec,
                    :deviceId, :startProgressPct, :endProgressPct
                )
                RETURNING %s
                """.formatted(COLUMNS);

        return jdbc.queryForObject(sql,
                new MapSqlParameterSource()
                        .addValue("userId",           s.getUserId())
                        .addValue("bookId",           s.getBookId())
                        .addValue("startedAt",        s.getStartedAt())
                        .addValue("endedAt",          s.getEndedAt())
                        .addValue("durationSec",      s.getDurationSec())
                        .addValue("deviceId",         s.getDeviceId())
                        .addValue("startProgressPct", s.getStartProgressPct())
                        .addValue("endProgressPct",   s.getEndProgressPct()),
                MAPPER);
    }

    @Override
    public List<ReadingSession> findRecentForUser(UUID userId, int limit) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM reading_sessions " +
                        "WHERE user_id = :uid " +
                        "ORDER BY started_at DESC LIMIT :lim",
                new MapSqlParameterSource().addValue("uid", userId).addValue("lim", limit),
                MAPPER);
    }

    @Override
    public List<ReadingSession> findByBook(UUID bookId, UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM reading_sessions " +
                        "WHERE book_id = :bid AND user_id = :uid " +
                        "ORDER BY started_at DESC",
                new MapSqlParameterSource().addValue("bid", bookId).addValue("uid", userId),
                MAPPER);
    }
}
