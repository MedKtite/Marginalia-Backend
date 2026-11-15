package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.NotificationQueueItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationQueueDaoImpl implements NotificationQueueDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<NotificationQueueItem> MAPPER =
            new BeanPropertyRowMapper<>(NotificationQueueItem.class);

    private static final String COLUMNS = """
            id, user_id, category, send_at, payload::text AS payload,
            status, attempts, last_error, created_at, sent_at
            """;

    /**
     * Same column list but every column is qualified with q. — needed in
     * RETURNING after an UPDATE … FROM join where unqualified names would
     * be ambiguous between the target table and the joined CTE.
     */
    private static final String COLUMNS_Q = """
            q.id, q.user_id, q.category, q.send_at, q.payload::text AS payload,
            q.status, q.attempts, q.last_error, q.created_at, q.sent_at
            """;

    /**
     * SELECT FOR UPDATE SKIP LOCKED with an immediate UPDATE bumps attempts
     * and keeps the rows visible only to this transaction — other dispatchers
     * skip past locked rows and pick different items.
     */
    @Override
    public List<NotificationQueueItem> claimDuePending(int limit) {
        String sql = """
                WITH claimed AS (
                    SELECT id FROM notification_queue
                    WHERE status = 'pending' AND send_at <= NOW()
                    ORDER BY send_at
                    LIMIT :lim
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE notification_queue q
                SET    attempts = q.attempts + 1
                FROM   claimed
                WHERE  q.id = claimed.id
                RETURNING %s
                """.formatted(COLUMNS_Q);
        return jdbc.query(sql,
                new MapSqlParameterSource("lim", limit),
                MAPPER);
    }

    @Override
    public NotificationQueueItem enqueue(UUID userId, String category,
                                         OffsetDateTime sendAt, String payloadJson) {
        String sql = """
                INSERT INTO notification_queue (user_id, category, send_at, payload, status)
                VALUES (:uid, :cat, :sendAt, CAST(:payload AS jsonb), 'pending')
                RETURNING %s
                """.formatted(COLUMNS);
        return jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("uid",     userId)
                .addValue("cat",     category)
                .addValue("sendAt",  sendAt)
                .addValue("payload", payloadJson), MAPPER);
    }

    @Override
    public void markSent(UUID id) {
        jdbc.update(
                "UPDATE notification_queue SET status = 'sent', sent_at = NOW(), last_error = NULL " +
                        "WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public void markFailed(UUID id, String error) {
        jdbc.update(
                "UPDATE notification_queue SET status = 'failed', last_error = :err WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("err", trim(error)));
    }

    @Override
    public void markSuppressed(UUID id, String reason) {
        jdbc.update(
                "UPDATE notification_queue SET status = 'suppressed', last_error = :r WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("r", trim(reason)));
    }

    @Override
    public boolean userHasRecentSend(UUID userId) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_queue " +
                        "WHERE user_id = :uid AND status = 'sent' " +
                        "  AND category <> 'billing' " +
                        "  AND sent_at >= NOW() - INTERVAL '24 hours'",
                new MapSqlParameterSource("uid", userId),
                Integer.class);
        return c != null && c > 0;
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
