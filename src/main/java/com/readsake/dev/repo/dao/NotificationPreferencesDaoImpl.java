package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.NotificationPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationPreferencesDaoImpl implements NotificationPreferencesDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<NotificationPreferences> MAPPER =
            new BeanPropertyRowMapper<>(NotificationPreferences.class);

    private static final String COLUMNS = """
            user_id, enabled, categories::text AS categories,
            ritual_time_local, passage_time_local,
            quiet_start_local, quiet_end_local, updated_at
            """;

    @Override
    public Optional<NotificationPreferences> findByUserId(UUID userId) {
        try {
            NotificationPreferences p = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM notification_preferences WHERE user_id = :uid",
                    new MapSqlParameterSource("uid", userId),
                    MAPPER);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public NotificationPreferences upsert(NotificationPreferences p) {
        String sql = """
                INSERT INTO notification_preferences (
                    user_id, enabled, categories,
                    ritual_time_local, passage_time_local,
                    quiet_start_local, quiet_end_local
                ) VALUES (
                    :userId,
                    COALESCE(:enabled, true),
                    COALESCE(CAST(:categories AS jsonb),
                             '{"ritual":true,"passage":true,"listening_resume":true,"sync":false,"milestone":false}'::jsonb),
                    COALESCE(:ritualTime,  19),
                    COALESCE(:passageTime, 8),
                    COALESCE(:quietStart,  22),
                    COALESCE(:quietEnd,    8)
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    enabled            = COALESCE(EXCLUDED.enabled,            notification_preferences.enabled),
                    categories         = COALESCE(EXCLUDED.categories,         notification_preferences.categories),
                    ritual_time_local  = COALESCE(EXCLUDED.ritual_time_local,  notification_preferences.ritual_time_local),
                    passage_time_local = COALESCE(EXCLUDED.passage_time_local, notification_preferences.passage_time_local),
                    quiet_start_local  = COALESCE(EXCLUDED.quiet_start_local,  notification_preferences.quiet_start_local),
                    quiet_end_local    = COALESCE(EXCLUDED.quiet_end_local,    notification_preferences.quiet_end_local),
                    updated_at         = NOW()
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId",       p.getUserId())
                .addValue("enabled",      p.getEnabled())
                .addValue("categories",   p.getCategories())
                .addValue("ritualTime",   p.getRitualTimeLocal())
                .addValue("passageTime",  p.getPassageTimeLocal())
                .addValue("quietStart",   p.getQuietStartLocal())
                .addValue("quietEnd",     p.getQuietEndLocal());

        return jdbc.queryForObject(sql, params, MAPPER);
    }
}
