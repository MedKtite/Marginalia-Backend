package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceDaoImpl implements DeviceDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Device> MAPPER =
            new BeanPropertyRowMapper<>(Device.class);

    private static final String COLUMNS = """
            id, user_id, platform, device_name, push_token,
            fcm_token_updated_at, push_permission,
            first_seen_at, last_seen_at
            """;

    @Override
    public Optional<Device> findByIdAndUser(UUID id, UUID userId) {
        try {
            Device d = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM devices WHERE id = :id AND user_id = :uid",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(d);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Device> findByPushToken(String token) {
        try {
            Device d = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM devices WHERE push_token = :t",
                    new MapSqlParameterSource("t", token),
                    MAPPER);
            return Optional.ofNullable(d);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Device> findByUser(UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM devices " +
                        "WHERE user_id = :uid " +
                        "ORDER BY last_seen_at DESC",
                new MapSqlParameterSource("uid", userId),
                MAPPER);
    }

    @Override
    public int countActiveByUser(UUID userId, int activeWithinDays) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM devices " +
                        "WHERE user_id = :uid " +
                        "  AND last_seen_at >= NOW() - make_interval(days => :d)",
                new MapSqlParameterSource().addValue("uid", userId).addValue("d", activeWithinDays),
                Integer.class);
        return c != null ? c : 0;
    }

    @Override
    public Device upsertByPushToken(Device d) {
        // If push_token already exists, the existing row is updated even if
        // the user_id differs — covers the "user re-signed-in on the same phone"
        // case. The schema doesn't enforce a unique constraint on push_token,
        // so we look up first and branch.
        if (d.getPushToken() != null) {
            Optional<Device> existing = findByPushToken(d.getPushToken());
            if (existing.isPresent()) {
                return updateForToken(existing.get().getId(), d);
            }
        }
        return insert(d);
    }

    @Override
    public Device insert(Device d) {
        String sql = """
                INSERT INTO devices (
                    user_id, platform, device_name, push_token,
                    fcm_token_updated_at, push_permission
                ) VALUES (
                    :userId, :platform, :deviceName, :pushToken,
                    CASE WHEN :pushToken IS NOT NULL THEN NOW() ELSE NULL END,
                    :pushPermission
                )
                RETURNING %s
                """.formatted(COLUMNS);

        return jdbc.queryForObject(sql, paramsFrom(d), MAPPER);
    }

    private Device updateForToken(UUID id, Device d) {
        String sql = """
                UPDATE devices SET
                    user_id              = :userId,
                    platform             = COALESCE(:platform,        platform),
                    device_name          = COALESCE(:deviceName,      device_name),
                    push_permission      = COALESCE(:pushPermission,  push_permission),
                    fcm_token_updated_at = NOW(),
                    last_seen_at         = NOW()
                WHERE id = :id
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource p = paramsFrom(d).addValue("id", id);
        return jdbc.queryForObject(sql, p, MAPPER);
    }

    @Override
    public boolean delete(UUID id, UUID userId) {
        int rows = jdbc.update(
                "DELETE FROM devices WHERE id = :id AND user_id = :uid",
                new MapSqlParameterSource().addValue("id", id).addValue("uid", userId));
        return rows > 0;
    }

    private MapSqlParameterSource paramsFrom(Device d) {
        return new MapSqlParameterSource()
                .addValue("userId",         d.getUserId())
                .addValue("platform",       d.getPlatform())
                .addValue("deviceName",     d.getDeviceName())
                .addValue("pushToken",      d.getPushToken())
                .addValue("pushPermission", d.getPushPermission());
    }
}
