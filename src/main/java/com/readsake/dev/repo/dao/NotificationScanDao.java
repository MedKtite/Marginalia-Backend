package com.readsake.dev.repo.dao;

import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregate queries that find users due for each notification
 * category right now. Each method returns a small payload-ready record so
 * the scheduler does no follow-up reads.
 *
 * Dedup is enforced inside each query (20h cooldown per user+category) so the
 * 15-minute scheduler tick can't double-enqueue when a user's local hour
 * stays inside the trigger window for multiple ticks.
 */
public interface NotificationScanDao {

    List<RitualCandidate> findRitualCandidates();

    List<PassageCandidate> findPassageCandidates();

    List<ListeningResumeCandidate> findListeningResumeCandidates();

    /** Evening ritual: user has an in-progress book, opened in the last 14 days. */
    record RitualCandidate(
            UUID userId,
            UUID bookId,
            String bookTitle,
            String bookAuthor
    ) {}

    /** Morning passage: random highlight from any of the user's books. */
    record PassageCandidate(
            UUID userId,
            UUID highlightId,
            UUID bookId,
            String passageText,
            String bookTitle,
            String bookAuthor
    ) {}

    /** Listening resume: user paused an audiobook 24–25h ago and hasn't returned. */
    record ListeningResumeCandidate(
            UUID userId,
            UUID bookId,
            String bookTitle,
            String bookAuthor,
            Double progressPct
    ) {}
}
