package com.marginalia.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GutenbergCatalogDaoImpl implements GutenbergCatalogDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public long count() {
        final Long n = jdbc.getJdbcOperations()
                .queryForObject("SELECT COUNT(*) FROM gutenberg_catalog", Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public void upsertBatch(List<Object[]> rows) {
        final String sql = """
                INSERT INTO gutenberg_catalog
                    (gutenberg_id, title, authors, language, subjects, title_norm, synced_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (gutenberg_id) DO UPDATE SET
                    title      = EXCLUDED.title,
                    authors    = EXCLUDED.authors,
                    language   = EXCLUDED.language,
                    subjects   = EXCLUDED.subjects,
                    title_norm = EXCLUDED.title_norm,
                    synced_at  = NOW()
                """;
        jdbc.getJdbcOperations().batchUpdate(sql, rows);
    }

    @Override
    public List<Map<String, Object>> search(String query, int limit) {
        // plainto_tsquery for word matches, trigram ILIKE as a prefix/typo net.
        final String sql = """
                SELECT gutenberg_id, title, authors, language
                FROM gutenberg_catalog
                WHERE search_tsv @@ plainto_tsquery('simple', :q)
                   OR title_norm LIKE :likeq
                ORDER BY (language = 'en') DESC,
                         ts_rank(search_tsv, plainto_tsquery('simple', :q)) DESC,
                         gutenberg_id
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("q", query)
                .addValue("likeq", "%" + com.marginalia.dev.services.TextNormalizer.normalize(query) + "%")
                .addValue("lim", limit));
    }

    @Override
    public List<Map<String, Object>> findByTitleNorm(String titleNorm) {
        final String sql = """
                SELECT gutenberg_id, title, authors, language
                FROM gutenberg_catalog
                WHERE title_norm = :tn
                LIMIT 20
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource().addValue("tn", titleNorm));
    }
}
