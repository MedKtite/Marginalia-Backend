package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Note;
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
public class NoteDaoImpl implements NoteDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Note> MAPPER =
            new BeanPropertyRowMapper<>(Note.class);

    private static final String COLUMNS = """
            id, user_id, book_id, highlight_id, body_md, created_at, updated_at
            """;

    @Override
    public Optional<Note> findByIdAndUser(UUID id, UUID userId) {
        try {
            Note n = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM notes WHERE id = :id AND user_id = :uid",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(n);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Note> findByBook(UUID bookId, UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM notes " +
                        "WHERE book_id = :bid AND user_id = :uid " +
                        "ORDER BY created_at DESC",
                new MapSqlParameterSource().addValue("bid", bookId).addValue("uid", userId),
                MAPPER);
    }

    @Override
    public List<Note> findByHighlight(UUID highlightId, UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM notes " +
                        "WHERE highlight_id = :hid AND user_id = :uid " +
                        "ORDER BY created_at",
                new MapSqlParameterSource().addValue("hid", highlightId).addValue("uid", userId),
                MAPPER);
    }

    @Override
    public Note insert(Note n) {
        String sql = """
                INSERT INTO notes (user_id, book_id, highlight_id, body_md)
                VALUES (:userId, :bookId, :highlightId, :bodyMd)
                RETURNING %s
                """.formatted(COLUMNS);

        return jdbc.queryForObject(sql,
                new MapSqlParameterSource()
                        .addValue("userId",      n.getUserId())
                        .addValue("bookId",      n.getBookId())
                        .addValue("highlightId", n.getHighlightId())
                        .addValue("bodyMd",      n.getBodyMd()),
                MAPPER);
    }

    @Override
    public Optional<Note> updateBody(UUID id, UUID userId, String bodyMd) {
        try {
            Note n = jdbc.queryForObject(
                    "UPDATE notes SET body_md = :body, updated_at = NOW() " +
                            "WHERE id = :id AND user_id = :uid " +
                            "RETURNING " + COLUMNS,
                    new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("uid", userId)
                            .addValue("body", bodyMd),
                    MAPPER);
            return Optional.ofNullable(n);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(UUID id, UUID userId) {
        int rows = jdbc.update(
                "DELETE FROM notes WHERE id = :id AND user_id = :uid",
                new MapSqlParameterSource().addValue("id", id).addValue("uid", userId));
        return rows > 0;
    }
}
