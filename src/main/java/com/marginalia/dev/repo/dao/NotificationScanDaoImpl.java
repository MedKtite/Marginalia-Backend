package com.marginalia.dev.repo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class NotificationScanDaoImpl implements NotificationScanDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────
    // Ritual
    // ─────────────────────────────────────────────────────────────
    private static final String RITUAL_SQL = """
            SELECT u.id          AS user_id,
                   b.id          AS book_id,
                   b.title       AS book_title,
                   b.author      AS book_author
            FROM   users u
            JOIN   notification_preferences np ON np.user_id = u.id
            JOIN   LATERAL (
                       SELECT id, title, author
                       FROM   books
                       WHERE  user_id = u.id
                         AND  status IN ('reading','listening')
                         AND  last_opened_at IS NOT NULL
                         AND  last_opened_at >= NOW() - INTERVAL '14 days'
                       ORDER BY last_opened_at DESC
                       LIMIT 1
                   ) b ON true
            WHERE  np.enabled = true
              AND  COALESCE((np.categories->>'ritual')::boolean, true) = true
              AND  EXTRACT(HOUR FROM (NOW() AT TIME ZONE u.timezone))::int
                       = np.ritual_time_local
              AND  NOT EXISTS (
                       SELECT 1 FROM notification_queue nq
                       WHERE  nq.user_id  = u.id
                         AND  nq.category = 'ritual'
                         AND  nq.created_at >= NOW() - INTERVAL '20 hours'
                   )
            """;

    private static final RowMapper<RitualCandidate> RITUAL_MAPPER = (rs, i) ->
            new RitualCandidate(
                    rs.getObject("user_id",  UUID.class),
                    rs.getObject("book_id",  UUID.class),
                    rs.getString("book_title"),
                    rs.getString("book_author"));

    @Override
    public List<RitualCandidate> findRitualCandidates() {
        return jdbc.query(RITUAL_SQL, RITUAL_MAPPER);
    }

    // ─────────────────────────────────────────────────────────────
    // Passage
    // ─────────────────────────────────────────────────────────────
    private static final String PASSAGE_SQL = """
            SELECT u.id          AS user_id,
                   h.id          AS highlight_id,
                   h.book_id     AS book_id,
                   h.passage_text AS passage_text,
                   b.title       AS book_title,
                   b.author      AS book_author
            FROM   users u
            JOIN   notification_preferences np ON np.user_id = u.id
            JOIN   LATERAL (
                       SELECT id, book_id, passage_text
                       FROM   highlights
                       WHERE  user_id = u.id
                         AND  passage_text IS NOT NULL
                       ORDER BY RANDOM()
                       LIMIT 1
                   ) h ON true
            JOIN   books b ON b.id = h.book_id
            WHERE  np.enabled = true
              AND  COALESCE((np.categories->>'passage')::boolean, true) = true
              AND  EXTRACT(HOUR FROM (NOW() AT TIME ZONE u.timezone))::int
                       = np.passage_time_local
              AND  NOT EXISTS (
                       SELECT 1 FROM notification_queue nq
                       WHERE  nq.user_id  = u.id
                         AND  nq.category = 'passage'
                         AND  nq.created_at >= NOW() - INTERVAL '20 hours'
                   )
            """;

    private static final RowMapper<PassageCandidate> PASSAGE_MAPPER = (rs, i) ->
            new PassageCandidate(
                    rs.getObject("user_id",      UUID.class),
                    rs.getObject("highlight_id", UUID.class),
                    rs.getObject("book_id",      UUID.class),
                    rs.getString("passage_text"),
                    rs.getString("book_title"),
                    rs.getString("book_author"));

    @Override
    public List<PassageCandidate> findPassageCandidates() {
        return jdbc.query(PASSAGE_SQL, PASSAGE_MAPPER);
    }

    // ─────────────────────────────────────────────────────────────
    // Listening resume
    // ─────────────────────────────────────────────────────────────
    //
    // Find audio books where the user's most recent reading_session ended
    // between 24h and 25h ago and no newer session exists for that book.
    // Per-book dedup (7 days) prevents re-prompting if the user ignored
    // the first nudge.
    private static final String LISTENING_RESUME_SQL = """
            SELECT DISTINCT ON (rs.user_id, rs.book_id)
                   u.id          AS user_id,
                   b.id          AS book_id,
                   b.title       AS book_title,
                   b.author      AS book_author,
                   b.progress_pct AS progress_pct
            FROM   users u
            JOIN   notification_preferences np ON np.user_id = u.id
            JOIN   reading_sessions rs ON rs.user_id = u.id
            JOIN   books b ON b.id = rs.book_id
            WHERE  np.enabled = true
              AND  COALESCE((np.categories->>'listening_resume')::boolean, true) = true
              AND  b.format IN ('m4b','mp3')
              AND  rs.ended_at >= NOW() - INTERVAL '25 hours'
              AND  rs.ended_at <= NOW() - INTERVAL '24 hours'
              AND  NOT EXISTS (
                       SELECT 1 FROM reading_sessions rs2
                       WHERE  rs2.user_id = rs.user_id
                         AND  rs2.book_id = rs.book_id
                         AND  rs2.ended_at > rs.ended_at
                   )
              AND  NOT EXISTS (
                       SELECT 1 FROM notification_queue nq
                       WHERE  nq.user_id  = u.id
                         AND  nq.category = 'listening_resume'
                         AND  nq.payload->>'book_id' = rs.book_id::text
                         AND  nq.created_at >= NOW() - INTERVAL '7 days'
                   )
            ORDER BY rs.user_id, rs.book_id, rs.ended_at DESC
            """;

    private static final RowMapper<ListeningResumeCandidate> LISTENING_MAPPER = (rs, i) -> {
        Double pct = rs.getObject("progress_pct") == null
                ? null
                : ((Number) rs.getObject("progress_pct")).doubleValue();
        return new ListeningResumeCandidate(
                rs.getObject("user_id",  UUID.class),
                rs.getObject("book_id",  UUID.class),
                rs.getString("book_title"),
                rs.getString("book_author"),
                pct);
    };

    @Override
    public List<ListeningResumeCandidate> findListeningResumeCandidates() {
        return jdbc.query(LISTENING_RESUME_SQL, LISTENING_MAPPER);
    }
}
