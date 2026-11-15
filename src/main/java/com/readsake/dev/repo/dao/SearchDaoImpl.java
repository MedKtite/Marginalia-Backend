package com.readsake.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SearchDaoImpl implements SearchDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public List<Map<String, Object>> searchBooks(UUID userId, String query, int limit) {
        // Books search uses pg_trgm-style ILIKE because there's no tsvector on books.
        // For 99% of cases this is enough; we can promote to tsvector later.
        String sql = """
                SELECT id, title, author, cover_url, cover_dominant_color, format, status
                FROM books
                WHERE user_id = :uid
                  AND (title ILIKE :likeq OR author ILIKE :likeq)
                ORDER BY last_opened_at DESC NULLS LAST
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("likeq", "%" + query + "%")
                .addValue("lim", limit));
    }

    @Override
    public List<Map<String, Object>> searchHighlights(UUID userId, String query, int limit) {
        // Uses the GIN-indexed search_tsv column. ts_headline returns the passage
        // with <mark>...</mark> wrapping the matched terms — the mobile app can
        // render those directly or strip them.
        String sql = """
                SELECT h.id,
                       h.book_id,
                       h.text_chapter_ref,
                       h.color_tag,
                       h.passage_text,
                       ts_headline(
                           'english',
                           h.passage_text,
                           plainto_tsquery('english', :q),
                           'StartSel=<mark>,StopSel=</mark>,MaxFragments=1,MaxWords=20,MinWords=5'
                       ) AS snippet,
                       ts_rank(h.search_tsv, plainto_tsquery('english', :q)) AS rank,
                       b.title  AS book_title,
                       b.author AS book_author
                FROM highlights h
                JOIN books b ON b.id = h.book_id
                WHERE h.user_id = :uid
                  AND h.search_tsv @@ plainto_tsquery('english', :q)
                ORDER BY rank DESC, h.created_at DESC
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("q", query)
                .addValue("lim", limit));
    }

    @Override
    public List<Map<String, Object>> searchNotes(UUID userId, String query, int limit) {
        String sql = """
                SELECT n.id,
                       n.book_id,
                       n.highlight_id,
                       ts_headline(
                           'english',
                           n.body_md,
                           plainto_tsquery('english', :q),
                           'StartSel=<mark>,StopSel=</mark>,MaxFragments=1,MaxWords=20,MinWords=5'
                       ) AS snippet,
                       ts_rank(n.search_tsv, plainto_tsquery('english', :q)) AS rank,
                       b.title AS book_title
                FROM notes n
                JOIN books b ON b.id = n.book_id
                WHERE n.user_id = :uid
                  AND n.search_tsv @@ plainto_tsquery('english', :q)
                ORDER BY rank DESC, n.created_at DESC
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("q", query)
                .addValue("lim", limit));
    }
}
