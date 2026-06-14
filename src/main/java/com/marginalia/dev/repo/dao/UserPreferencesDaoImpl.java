package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.UserPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserPreferencesDaoImpl implements UserPreferencesDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<UserPreferences> MAPPER =
            new BeanPropertyRowMapper<>(UserPreferences.class);

    private static final String COLUMNS = """
            user_id, theme, density, greeting_override,
            home_sections::text AS home_sections,
            reading_font_size, reading_line_height, updated_at
            """;

    @Override
    public Optional<UserPreferences> findByUserId(UUID userId) {
        try {
            UserPreferences p = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM user_preferences WHERE user_id = :uid",
                    new MapSqlParameterSource("uid", userId),
                    MAPPER);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public UserPreferences upsert(UserPreferences p) {
        // Defaults applied here so the DB DEFAULT clause fires only on first-time INSERT.
        String sql = """
                INSERT INTO user_preferences (
                    user_id, theme, density, greeting_override, home_sections,
                    reading_font_size, reading_line_height
                ) VALUES (
                    :userId,
                    COALESCE(:theme,            'system'),
                    COALESCE(:density,          'spacious'),
                    :greetingOverride,
                    COALESCE(CAST(:homeSections AS jsonb),
                             '{"passage_of_day":true,"continue_listening":true,"also_in_progress":true}'::jsonb),
                    COALESCE(:readingFontSize,  17),
                    COALESCE(:readingLineHeight,165)
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    theme               = COALESCE(EXCLUDED.theme,              user_preferences.theme),
                    density             = COALESCE(EXCLUDED.density,            user_preferences.density),
                    greeting_override   = COALESCE(EXCLUDED.greeting_override,  user_preferences.greeting_override),
                    home_sections       = COALESCE(EXCLUDED.home_sections,      user_preferences.home_sections),
                    reading_font_size   = COALESCE(EXCLUDED.reading_font_size,  user_preferences.reading_font_size),
                    reading_line_height = COALESCE(EXCLUDED.reading_line_height,user_preferences.reading_line_height),
                    updated_at          = NOW()
                RETURNING %s
                """.formatted(COLUMNS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId",            p.getUserId())
                .addValue("theme",             p.getTheme())
                .addValue("density",           p.getDensity())
                .addValue("greetingOverride",  p.getGreetingOverride())
                .addValue("homeSections",      p.getHomeSections())
                .addValue("readingFontSize",   p.getReadingFontSize())
                .addValue("readingLineHeight", p.getReadingLineHeight());

        return jdbc.queryForObject(sql, params, MAPPER);
    }
}
