package com.marginalia.dev.repo.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class NotificationLogDaoImpl implements NotificationLogDao {

    private static final Logger log = LoggerFactory.getLogger(NotificationLogDaoImpl.class);

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public void record(UUID userId,
                       String category,
                       String status,
                       String fcmMessageId,
                       UUID deviceId,
                       String payloadJson) {
        try {
            jdbc.update("""
                    INSERT INTO notification_log
                        (user_id, category, fcm_message_id, device_id, status, payload)
                    VALUES
                        (:uid, :cat, :fcm, :did, :status, CAST(:payload AS jsonb))
                    """,
                    new MapSqlParameterSource()
                            .addValue("uid",     userId)
                            .addValue("cat",     category)
                            .addValue("fcm",     fcmMessageId)
                            .addValue("did",     deviceId)
                            .addValue("status",  status)
                            .addValue("payload", payloadJson));
        } catch (Exception e) {
            // Logging the log shouldn't bring down the dispatcher. Most likely
            // cause is a missing future partition — the daily scheduler keeps
            // them current, and on startup we ensure today's exists.
            log.warn("notification_log insert failed (user={}, cat={}, status={}): {}",
                    userId, category, status, e.getMessage());
        }
    }
}
