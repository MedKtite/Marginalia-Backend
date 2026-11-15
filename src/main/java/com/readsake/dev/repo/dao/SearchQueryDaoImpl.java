package com.readsake.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SearchQueryDaoImpl implements SearchQueryDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public void insert(UUID userId, String query) {
        final String sql = """
                INSERT INTO search_queries (user_id, query)
                VALUES (:userId, :query)
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("query", query));
    }

    @Override
    public List<String> recentDistinct(UUID userId, int limit) {
        // GROUP BY collapses repeats of the same query; MAX(searched_at) keeps
        // the ordering by when the user last cared about it.
        final String sql = """
                SELECT query
                FROM search_queries
                WHERE user_id = :userId
                GROUP BY query
                ORDER BY MAX(searched_at) DESC
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("lim", limit), String.class);
    }
}
