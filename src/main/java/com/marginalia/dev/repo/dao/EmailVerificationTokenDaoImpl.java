package com.marginalia.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailVerificationTokenDaoImpl implements EmailVerificationTokenDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional
    public void replaceForUser(UUID userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbc.update(
                "DELETE FROM email_verification_tokens WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId));
        jdbc.update(
                """
                INSERT INTO email_verification_tokens (user_id, token_hash, expires_at)
                VALUES (:userId, :tokenHash, :expiresAt)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("tokenHash", tokenHash)
                        .addValue("expiresAt", expiresAt));
    }

    @Override
    public Optional<UUID> consume(String tokenHash) {
        List<UUID> ids = jdbc.query(
                """
                DELETE FROM email_verification_tokens
                WHERE token_hash = :tokenHash AND expires_at > NOW()
                RETURNING user_id
                """,
                new MapSqlParameterSource("tokenHash", tokenHash),
                (rs, rowNum) -> rs.getObject("user_id", UUID.class));
        return ids.stream().findFirst();
    }
}
