package com.readsake.dev.scheduled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.readsake.dev.repo.dao.NotificationQueueDao;
import com.readsake.dev.repo.dao.NotificationScanDao;
import com.readsake.dev.repo.dao.NotificationScanDao.ListeningResumeCandidate;
import com.readsake.dev.repo.dao.NotificationScanDao.PassageCandidate;
import com.readsake.dev.repo.dao.NotificationScanDao.RitualCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enqueues ritual / passage / listening-resume rows into notification_queue
 * based on each user's local time and recent activity. The {@link NotificationDispatcher}
 * then handles delivery and final-mile suppression (quiet hours, frequency cap,
 * per-category opt-out).
 *
 * Cadence:
 *   ritual           — every 15 min (default ritual_time_local = 19)
 *   passage          — every 15 min, offset by 5 (default passage_time_local = 8)
 *   listening_resume — hourly
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    @Autowired
    private NotificationScanDao scanDao;

    @Autowired
    private NotificationQueueDao queueDao;

    private final ObjectMapper mapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // Ritual
    // ─────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0/15 * * * *")
    public void scanForRituals() {
        List<RitualCandidate> due = scanDao.findRitualCandidates();
        if (due.isEmpty()) return;

        log.info("Ritual scan: {} candidate(s)", due.size());
        OffsetDateTime now = OffsetDateTime.now();
        for (RitualCandidate c : due) {
            try {
                String title = ritualTitle(c);
                String body  = ritualBody(c);
                String payload = payloadJson(title, body, p -> {
                    p.put("type", "ritual");
                    p.put("book_id", c.bookId().toString());
                });
                queueDao.enqueue(c.userId(), "ritual", now, payload);
            } catch (Exception e) {
                log.warn("Failed to enqueue ritual for user {}: {}", c.userId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Passage
    // ─────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 5/15 * * * *")
    public void scanForPassages() {
        List<PassageCandidate> due = scanDao.findPassageCandidates();
        if (due.isEmpty()) return;

        log.info("Passage scan: {} candidate(s)", due.size());
        OffsetDateTime now = OffsetDateTime.now();
        for (PassageCandidate c : due) {
            try {
                String snippet = truncate(c.passageText(), 140);
                String title = c.bookTitle() == null
                        ? "A passage from your library"
                        : "From " + c.bookTitle();
                String payload = payloadJson(title, snippet, p -> {
                    p.put("type", "passage");
                    p.put("book_id", c.bookId().toString());
                    p.put("highlight_id", c.highlightId().toString());
                });
                queueDao.enqueue(c.userId(), "passage", now, payload);
            } catch (Exception e) {
                log.warn("Failed to enqueue passage for user {}: {}", c.userId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Listening resume
    // ─────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 10 * * * *")
    public void scanForListeningResume() {
        List<ListeningResumeCandidate> due = scanDao.findListeningResumeCandidates();
        if (due.isEmpty()) return;

        log.info("Listening-resume scan: {} candidate(s)", due.size());
        OffsetDateTime now = OffsetDateTime.now();
        for (ListeningResumeCandidate c : due) {
            try {
                String title = c.bookTitle() == null
                        ? "Pick up where you left off?"
                        : "Return to " + c.bookTitle() + "?";
                String body  = listeningResumeBody(c);
                String payload = payloadJson(title, body, p -> {
                    p.put("type", "listening_resume");
                    p.put("book_id", c.bookId().toString());
                });
                queueDao.enqueue(c.userId(), "listening_resume", now, payload);
            } catch (Exception e) {
                log.warn("Failed to enqueue listening-resume for user {}: {}",
                        c.userId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Copy templates
    // ─────────────────────────────────────────────────────────────
    private static String ritualTitle(RitualCandidate c) {
        if (c.bookTitle() == null) return "Your evening with Readsake";
        // Two templates, deterministic per user so it isn't jarring day-to-day.
        return (c.userId().getLeastSignificantBits() & 1) == 0
                ? c.bookTitle() + " is waiting."
                : "Continue " + c.bookTitle() + "?";
    }

    private static String ritualBody(RitualCandidate c) {
        return c.bookAuthor() != null
                ? "Settle in with " + c.bookAuthor() + "."
                : "Settle in. The page is still warm.";
    }

    private static String listeningResumeBody(ListeningResumeCandidate c) {
        if (c.progressPct() != null) {
            return String.format("You paused yesterday at %.0f%%.", c.progressPct());
        }
        return "You paused yesterday. Ready to keep listening?";
    }

    // ─────────────────────────────────────────────────────────────
    // Payload helpers
    // ─────────────────────────────────────────────────────────────
    /**
     * Build a JSON string in the shape the dispatcher expects:
     *   { "title": "...", "body": "...", "data": { ... } }
     */
    private String payloadJson(String title, String body, DataFiller filler) {
        ObjectNode root = mapper.createObjectNode();
        root.put("title", title);
        root.put("body",  body);
        ObjectNode data = root.putObject("data");
        filler.fill(data);
        return root.toString();
    }

    @FunctionalInterface
    private interface DataFiller {
        void fill(ObjectNode data);
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        s = s.strip();
        return s.length() <= n ? s : s.substring(0, n - 1).stripTrailing() + "…";
    }

    // Reserved for future use — referenced by toString of UUID in payload.
    @SuppressWarnings("unused")
    private static String safeUuid(UUID u) { return u == null ? null : u.toString(); }
}
