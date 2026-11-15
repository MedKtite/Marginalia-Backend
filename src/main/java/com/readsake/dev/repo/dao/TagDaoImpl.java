package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Tag;
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
public class TagDaoImpl implements TagDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Tag> MAPPER =
            new BeanPropertyRowMapper<>(Tag.class);

    private static final String COLUMNS = "id, user_id, name, color_hex, is_system, created_at";

    @Override
    public List<Tag> findSystemTags() {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM tags WHERE is_system = true ORDER BY created_at",
                MAPPER);
    }

    @Override
    public Optional<Tag> findSystemByName(String name) {
        try {
            Tag t = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM tags " +
                            "WHERE is_system = true AND lower(name) = lower(:name)",
                    new MapSqlParameterSource("name", name),
                    MAPPER);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Tag> findVisibleForUser(UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM tags " +
                        "WHERE is_system = true OR user_id = :uid " +
                        "ORDER BY is_system DESC, created_at",
                new MapSqlParameterSource("uid", userId),
                MAPPER);
    }

    @Override
    public Optional<Tag> findVisibleByIdForUser(UUID id, UUID userId) {
        try {
            Tag t = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM tags " +
                            "WHERE id = :id AND (is_system = true OR user_id = :uid)",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Tag> findOwnedByUser(UUID id, UUID userId) {
        try {
            Tag t = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM tags " +
                            "WHERE id = :id AND user_id = :uid AND is_system = false",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Tag insertCustom(UUID userId, String name, String colorHex) {
        String sql = """
                INSERT INTO tags (user_id, name, color_hex, is_system)
                VALUES (:uid, :name, :color, false)
                RETURNING %s
                """.formatted(COLUMNS);
        return jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("uid",   userId)
                .addValue("name",  name)
                .addValue("color", colorHex), MAPPER);
    }

    @Override
    public Optional<Tag> updateCustom(UUID id, UUID userId, String name, String colorHex) {
        String sql = """
                UPDATE tags SET
                    name      = COALESCE(:name,  name),
                    color_hex = COALESCE(:color, color_hex)
                WHERE  id = :id AND user_id = :uid AND is_system = false
                RETURNING %s
                """.formatted(COLUMNS);
        try {
            Tag t = jdbc.queryForObject(sql, new MapSqlParameterSource()
                    .addValue("id",    id)
                    .addValue("uid",   userId)
                    .addValue("name",  name)
                    .addValue("color", colorHex), MAPPER);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteCustom(UUID id, UUID userId) {
        int rows = jdbc.update(
                "DELETE FROM tags WHERE id = :id AND user_id = :uid AND is_system = false",
                new MapSqlParameterSource().addValue("id", id).addValue("uid", userId));
        return rows > 0;
    }

    @Override
    public void attachToHighlight(UUID highlightId, UUID tagId) {
        jdbc.update(
                "INSERT INTO highlight_tags (highlight_id, tag_id) VALUES (:hid, :tid) " +
                        "ON CONFLICT DO NOTHING",
                new MapSqlParameterSource().addValue("hid", highlightId).addValue("tid", tagId));
    }

    @Override
    public boolean detachFromHighlight(UUID highlightId, UUID tagId) {
        int rows = jdbc.update(
                "DELETE FROM highlight_tags WHERE highlight_id = :hid AND tag_id = :tid",
                new MapSqlParameterSource().addValue("hid", highlightId).addValue("tid", tagId));
        return rows > 0;
    }

    @Override
    public java.util.Map<UUID, Integer> countHighlightsByTag(UUID userId) {
        var counts = new java.util.LinkedHashMap<UUID, Integer>();
        jdbc.query(
                "SELECT ht.tag_id, COUNT(*) AS n " +
                        "FROM highlight_tags ht " +
                        "JOIN highlights h ON h.id = ht.highlight_id " +
                        "WHERE h.user_id = :uid " +
                        "GROUP BY ht.tag_id",
                new MapSqlParameterSource("uid", userId),
                rs -> { counts.put(rs.getObject("tag_id", UUID.class), rs.getInt("n")); });
        return counts;
    }

    @Override
    public List<Tag> findByHighlight(UUID highlightId) {
        return jdbc.query(
                "SELECT t.id, t.user_id, t.name, t.color_hex, t.is_system, t.created_at " +
                        "FROM   tags t " +
                        "JOIN   highlight_tags ht ON ht.tag_id = t.id " +
                        "WHERE  ht.highlight_id = :hid " +
                        "ORDER BY t.is_system DESC, t.created_at",
                new MapSqlParameterSource("hid", highlightId),
                MAPPER);
    }
}
