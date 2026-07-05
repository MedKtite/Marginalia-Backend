package com.marginalia.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class BookRequestDaoImpl implements BookRequestDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Override
    public void insert(UUID userId, String title, String author, String isbn13, String googleId) {
        final String sql = """
                INSERT INTO book_requests (user_id, title, author, isbn13, google_id)
                VALUES (:userId, :title, :author, :isbn13, :googleId)
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("title", title)
                .addValue("author", author)
                .addValue("isbn13", isbn13)
                .addValue("googleId", googleId));
    }
}
