package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<User> MAPPER = new BeanPropertyRowMapper<>(User.class);

    private static final String COLUMNS = """
            id, email, email_verified, display_name, short_name, avatar_initial,
            password_hash, oauth_provider, oauth_subject, timezone,
            created_at, updated_at, last_seen_at
            """;

    @Override
    public Optional<User> findById(UUID id) {
        try {
            User u = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM users WHERE id = :id",
                    new MapSqlParameterSource("id", id),
                    MAPPER);
            return Optional.ofNullable(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            User u = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM users WHERE lower(email) = lower(:email)",
                    new MapSqlParameterSource("email", email),
                    MAPPER);
            return Optional.ofNullable(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByOauth(String provider, String subject) {
        try {
            User u = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM users " +
                            "WHERE oauth_provider = :p AND oauth_subject = :s",
                    new MapSqlParameterSource().addValue("p", provider).addValue("s", subject),
                    MAPPER);
            return Optional.ofNullable(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User insert(User user) {
        String sql = """
                INSERT INTO users (
                    email, email_verified, display_name, short_name, avatar_initial,
                    password_hash, oauth_provider, oauth_subject, timezone
                ) VALUES (
                    :email, :emailVerified, :displayName, :shortName, :avatarInitial,
                    :passwordHash, :oauthProvider, :oauthSubject, :timezone
                )
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email",         user.getEmail())
                .addValue("emailVerified", user.getEmailVerified() != null ? user.getEmailVerified() : false)
                .addValue("displayName",   user.getDisplayName())
                .addValue("shortName",     user.getShortName())
                .addValue("avatarInitial", user.getAvatarInitial())
                .addValue("passwordHash",  user.getPasswordHash())
                .addValue("oauthProvider", user.getOauthProvider())
                .addValue("oauthSubject",  user.getOauthSubject())
                .addValue("timezone",      user.getTimezone() != null ? user.getTimezone() : "UTC");

        return jdbc.queryForObject(sql, params, MAPPER);
    }

    @Override
    public void updateLastSeen(UUID userId) {
        jdbc.update(
                "UPDATE users SET last_seen_at = :now WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("now", OffsetDateTime.now())
                        .addValue("id", userId));
    }
}
