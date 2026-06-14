package com.marginalia.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class InsightsDaoImpl implements InsightsDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public int countBooksFinishedThisYear(UUID userId) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM books " +
                        "WHERE user_id = :uid AND status = 'finished' " +
                        "  AND finished_at >= date_trunc('year', NOW())",
                new MapSqlParameterSource("uid", userId),
                Integer.class);
        return c != null ? c : 0;
    }

    @Override
    public int countTotalHighlights(UUID userId) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM highlights WHERE user_id = :uid",
                new MapSqlParameterSource("uid", userId),
                Integer.class);
        return c != null ? c : 0;
    }

    @Override
    public int minutesReadInLastDays(UUID userId, int days) {
        Long secs = jdbc.queryForObject(
                "SELECT COALESCE(SUM(duration_sec), 0) FROM reading_sessions " +
                        "WHERE user_id = :uid " +
                        "  AND started_at >= NOW() - make_interval(days => :days)",
                new MapSqlParameterSource().addValue("uid", userId).addValue("days", days),
                Long.class);
        return secs == null ? 0 : (int) (secs / 60);
    }

    @Override
    public List<LocalDate> findSessionDaysDesc(UUID userId, int lookbackDays) {
        // Truncate in UTC — same boundary the streak algorithm assumes in the service.
        return jdbc.query(
                "SELECT DISTINCT (started_at AT TIME ZONE 'UTC')::date AS day " +
                        "FROM reading_sessions " +
                        "WHERE user_id = :uid " +
                        "  AND started_at >= NOW() - make_interval(days => :look) " +
                        "ORDER BY day DESC",
                new MapSqlParameterSource().addValue("uid", userId).addValue("look", lookbackDays),
                (rs, i) -> rs.getObject("day", LocalDate.class));
    }

    @Override
    public List<Map<String, Object>> tagBreakdown(UUID userId) {
        // LEFT JOIN ensures all 7 system tags appear even with count=0.
        String sql = """
                SELECT t.name        AS name,
                       t.color_hex   AS color_hex,
                       COALESCE(c.cnt, 0) AS count
                FROM tags t
                LEFT JOIN (
                    SELECT color_tag, COUNT(*) AS cnt
                    FROM highlights
                    WHERE user_id = :uid
                    GROUP BY color_tag
                ) c ON c.color_tag = t.name
                WHERE t.is_system = true
                ORDER BY count DESC, t.created_at
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("uid", userId));
    }
}
