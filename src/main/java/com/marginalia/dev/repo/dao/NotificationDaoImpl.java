package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.NotificationItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class NotificationDaoImpl implements NotificationDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<NotificationItem> MAPPER =
            new BeanPropertyRowMapper<>(NotificationItem.class);

    private static final String COLUMNS = """
            id, user_id, category, title, body, data::text AS data, read_at, created_at
            """;

    private static final Set<String> ACTIVITY_CATEGORIES = Set.of(
            "quote_saved", "chapter_milestone", "system_activity", "sync", "billing"
    );

    @Override
    public NotificationItem create(UUID userId, String category, String title, String body, String dataJson) {
        String sql = """
                INSERT INTO notifications (user_id, category, title, body, data)
                VALUES (:userId, :category, :title, :body, CAST(:data AS jsonb))
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("category", category)
                .addValue("title", title)
                .addValue("body", body)
                .addValue("data", dataJson != null ? dataJson : "{}");

        return jdbc.queryForObject(sql, params, MAPPER);
    }

    @Override
    public Optional<NotificationItem> findById(UUID id, UUID userId) {
        String sql = """
                SELECT %s FROM notifications
                WHERE id = :id AND user_id = :userId
                """.formatted(COLUMNS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);

        return jdbc.query(sql, params, MAPPER).stream().findFirst();
    }

    @Override
    public List<NotificationItem> findByUser(UUID userId, String tab, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM notifications WHERE user_id = :userId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit)
                .addValue("offset", offset);

        if ("unread".equalsIgnoreCase(tab)) {
            sql.append(" AND read_at IS NULL");
        } else if ("activity".equalsIgnoreCase(tab)) {
            sql.append(" AND category IN (:categories)");
            params.addValue("categories", ACTIVITY_CATEGORIES);
        }

        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        return jdbc.query(sql.toString(), params, MAPPER);
    }

    @Override
    public long countUnread(UUID userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read_at IS NULL";
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("userId", userId), Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public boolean markAsRead(UUID id, UUID userId, OffsetDateTime readAt) {
        String sql = """
                UPDATE notifications
                SET read_at = COALESCE(read_at, :readAt)
                WHERE id = :id AND user_id = :userId
                """;
        int rows = jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("readAt", readAt));
        return rows > 0;
    }

    @Override
    public int markAllAsRead(UUID userId, OffsetDateTime readAt) {
        String sql = """
                UPDATE notifications
                SET read_at = :readAt
                WHERE user_id = :userId AND read_at IS NULL
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("readAt", readAt));
    }

    @Override
    public boolean delete(UUID id, UUID userId) {
        String sql = "DELETE FROM notifications WHERE id = :id AND user_id = :userId";
        int rows = jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId));
        return rows > 0;
    }
}
