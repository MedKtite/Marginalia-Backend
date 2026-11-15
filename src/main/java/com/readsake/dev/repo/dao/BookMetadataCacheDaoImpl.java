package com.readsake.dev.repo.dao;
import com.readsake.dev.entities.BookMetadataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public class BookMetadataCacheDaoImpl implements BookMetadataCacheDao {
    @Autowired private NamedParameterJdbcTemplate jdbc;
    private static final BeanPropertyRowMapper<BookMetadataCache> MAPPER = new BeanPropertyRowMapper<>(BookMetadataCache.class);
    private static final String COLUMNS = "isbn_13 AS isbn13, source, title, authors, publisher, published_year AS publishedYear, page_count AS pageCount, description, cover_url AS coverUrl, raw::text AS raw, fetched_at AS fetchedAt, expires_at AS expiresAt";
    @Override public Optional<BookMetadataCache> findFresh(String isbn13) { try { return Optional.ofNullable(jdbc.queryForObject("SELECT " + COLUMNS + " FROM book_metadata_cache WHERE isbn_13=:isbn AND expires_at > NOW()", new MapSqlParameterSource("isbn", isbn13), MAPPER)); } catch (EmptyResultDataAccessException e) { return Optional.empty(); } }
    @Override public void upsert(BookMetadataCache c) {
        String sql = "INSERT INTO book_metadata_cache (isbn_13,source,title,authors,publisher,published_year,page_count,description,cover_url,raw,fetched_at,expires_at) VALUES (:isbn,:source,:title,CAST(:authors AS text[]),:publisher,:year,:pages,:description,:cover,CAST(:raw AS jsonb),NOW(),NOW()+INTERVAL '30 days') ON CONFLICT (isbn_13) DO UPDATE SET source=EXCLUDED.source,title=COALESCE(book_metadata_cache.title,EXCLUDED.title),authors=COALESCE(book_metadata_cache.authors,EXCLUDED.authors),publisher=COALESCE(book_metadata_cache.publisher,EXCLUDED.publisher),published_year=COALESCE(book_metadata_cache.published_year,EXCLUDED.published_year),page_count=COALESCE(book_metadata_cache.page_count,EXCLUDED.page_count),description=COALESCE(book_metadata_cache.description,EXCLUDED.description),cover_url=COALESCE(book_metadata_cache.cover_url,EXCLUDED.cover_url),raw=EXCLUDED.raw,fetched_at=NOW(),expires_at=NOW()+INTERVAL '30 days'";
        jdbc.update(sql, new MapSqlParameterSource().addValue("isbn", c.getIsbn13()).addValue("source", c.getSource()).addValue("title", c.getTitle()).addValue("authors", pgArray(c.getAuthors())).addValue("publisher", c.getPublisher()).addValue("year", c.getPublishedYear()).addValue("pages", c.getPageCount()).addValue("description", c.getDescription()).addValue("cover", c.getCoverUrl()).addValue("raw", c.getRaw() == null ? "{}" : c.getRaw()));
    }
    private static String pgArray(String[] values) { if (values == null || values.length == 0) return "{}"; return "{" + String.join(",", java.util.Arrays.stream(values).map(v -> "\"" + v.replace("\"", "\\\"") + "\"").toList()) + "}"; }
}
