package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Highlight;
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
public class HighlightDaoImpl implements HighlightDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Highlight> MAPPER =
            new BeanPropertyRowMapper<>(Highlight.class);

    private static final String COLUMNS = """
            id, user_id, book_id,
            text_chapter_ref, text_start_offset, text_end_offset, passage_text,
            audio_start_sec, audio_end_sec,
            color_tag, created_at, updated_at
            """;

    @Override
    public Optional<Highlight> findByIdAndUser(UUID id, UUID userId) {
        try {
            Highlight h = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM highlights WHERE id = :id AND user_id = :uid",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(h);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Highlight> findByBook(UUID bookId, UUID userId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM highlights " +
                        "WHERE book_id = :bid AND user_id = :uid " +
                        "ORDER BY COALESCE(text_start_offset, 0), audio_start_sec NULLS LAST, created_at",
                new MapSqlParameterSource().addValue("bid", bookId).addValue("uid", userId),
                MAPPER);
    }

    @Override
    public List<Highlight> findRecentForUser(UUID userId, int limit) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM highlights " +
                        "WHERE user_id = :uid " +
                        "ORDER BY created_at DESC LIMIT :lim",
                new MapSqlParameterSource().addValue("uid", userId).addValue("lim", limit),
                MAPPER);
    }

    @Override
    public Highlight insert(Highlight h) {
        String sql = """
                INSERT INTO highlights (
                    user_id, book_id,
                    text_chapter_ref, text_start_offset, text_end_offset, passage_text,
                    audio_start_sec, audio_end_sec,
                    color_tag
                ) VALUES (
                    :userId, :bookId,
                    :textChapterRef, :textStartOffset, :textEndOffset, :passageText,
                    :audioStartSec, :audioEndSec,
                    :colorTag
                )
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("userId",          h.getUserId())
                .addValue("bookId",          h.getBookId())
                .addValue("textChapterRef",  h.getTextChapterRef())
                .addValue("textStartOffset", h.getTextStartOffset())
                .addValue("textEndOffset",   h.getTextEndOffset())
                .addValue("passageText",     h.getPassageText())
                .addValue("audioStartSec",   h.getAudioStartSec())
                .addValue("audioEndSec",     h.getAudioEndSec())
                .addValue("colorTag",        h.getColorTag());

        return jdbc.queryForObject(sql, p, MAPPER);
    }

    @Override
    public Optional<Highlight> patchColorTag(UUID id, UUID userId, String colorTag) {
        try {
            Highlight h = jdbc.queryForObject(
                    "UPDATE highlights SET color_tag = :tag, updated_at = NOW() " +
                            "WHERE id = :id AND user_id = :uid " +
                            "RETURNING " + COLUMNS,
                    new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("uid", userId)
                            .addValue("tag", colorTag),
                    MAPPER);
            return Optional.ofNullable(h);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(UUID id, UUID userId) {
        int rows = jdbc.update(
                "DELETE FROM highlights WHERE id = :id AND user_id = :uid",
                new MapSqlParameterSource().addValue("id", id).addValue("uid", userId));
        return rows > 0;
    }
}
