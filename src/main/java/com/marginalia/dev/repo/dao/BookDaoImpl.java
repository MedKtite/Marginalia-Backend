package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BookDaoImpl implements BookDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Book> MAPPER = new BeanPropertyRowMapper<>(Book.class);

    private static final String COLUMNS = """
            id, user_id, title, author, narrator, publisher, published_year,
            isbn_13 AS isbn13, page_count, duration_sec, language,
            format, file_key, file_size_bytes, cover_url, cover_dominant_color,
            status, progress_pct, cursor::text AS cursor,
            last_opened_at, finished_at, created_at, updated_at
            """;

    @Override
    public Optional<Book> findByIdAndUser(UUID id, UUID userId) {
        try {
            Book b = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM books WHERE id = :id AND user_id = :uid",
                    new MapSqlParameterSource().addValue("id", id).addValue("uid", userId),
                    MAPPER);
            return Optional.ofNullable(b);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Book> findByUser(UUID userId, String status) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM books WHERE user_id = :uid");
        MapSqlParameterSource params = new MapSqlParameterSource("uid", userId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }
        sql.append(" ORDER BY last_opened_at DESC NULLS LAST, created_at DESC");
        return jdbc.query(sql.toString(), params, MAPPER);
    }

    @Override
    public int countByUser(UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM books WHERE user_id = :uid",
                new MapSqlParameterSource("uid", userId),
                Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public Book insert(Book b) {
        String sql = """
                INSERT INTO books (
                    user_id, title, author, narrator, publisher, published_year,
                    isbn_13, page_count, duration_sec, language,
                    format, file_key, file_size_bytes, cover_url, cover_dominant_color,
                    status, progress_pct, cursor, last_opened_at
                ) VALUES (
                    :userId, :title, :author, :narrator, :publisher, :publishedYear,
                    :isbn13, :pageCount, :durationSec, :language,
                    :format, :fileKey, :fileSizeBytes, :coverUrl, :coverDominantColor,
                    :status, :progressPct, CAST(:cursor AS jsonb), :lastOpenedAt
                )
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("userId",             b.getUserId())
                .addValue("title",              b.getTitle())
                .addValue("author",             b.getAuthor())
                .addValue("narrator",           b.getNarrator())
                .addValue("publisher",          b.getPublisher())
                .addValue("publishedYear",      b.getPublishedYear())
                .addValue("isbn13",             b.getIsbn13())
                .addValue("pageCount",          b.getPageCount())
                .addValue("durationSec",        b.getDurationSec())
                .addValue("language",           b.getLanguage())
                .addValue("format",             b.getFormat())
                .addValue("fileKey",            b.getFileKey())
                .addValue("fileSizeBytes",      b.getFileSizeBytes())
                .addValue("coverUrl",           b.getCoverUrl())
                .addValue("coverDominantColor", b.getCoverDominantColor())
                .addValue("status",             b.getStatus() != null ? b.getStatus() : "reading")
                .addValue("progressPct",        b.getProgressPct())
                .addValue("cursor",             b.getCursor())
                .addValue("lastOpenedAt",       b.getLastOpenedAt());

        return jdbc.queryForObject(sql, p, MAPPER);
    }

    @Override
    public Optional<Book> patch(UUID id, UUID userId,
                                String status,
                                BigDecimal progressPct,
                                String cursorJson,
                                boolean touchLastOpened) {
        // finished_at auto-set when status transitions to 'finished'.
        OffsetDateTime now = OffsetDateTime.now();

        String sql = """
                UPDATE books SET
                    status         = COALESCE(:status,       status),
                    progress_pct   = COALESCE(:progressPct,  progress_pct),
                    cursor         = COALESCE(CAST(:cursor AS jsonb), cursor),
                    last_opened_at = CASE WHEN :touch THEN :now ELSE last_opened_at END,
                    finished_at    = CASE WHEN :status = 'finished' AND finished_at IS NULL
                                          THEN :now ELSE finished_at END,
                    updated_at     = NOW()
                WHERE id = :id AND user_id = :uid
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id",           id)
                .addValue("uid",          userId)
                .addValue("status",       status)
                .addValue("progressPct",  progressPct)
                .addValue("cursor",       cursorJson)
                .addValue("touch",        touchLastOpened)
                .addValue("now",          now);

        try {
            Book b = jdbc.queryForObject(sql, p, MAPPER);
            return Optional.ofNullable(b);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(UUID id, UUID userId) {
        int rows = jdbc.update(
                "DELETE FROM books WHERE id = :id AND user_id = :uid",
                new MapSqlParameterSource().addValue("id", id).addValue("uid", userId));
        return rows > 0;
    }
}
