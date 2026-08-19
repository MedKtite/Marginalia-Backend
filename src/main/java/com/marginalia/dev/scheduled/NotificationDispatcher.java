package com.marginalia.dev.scheduled;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marginalia.dev.entities.Device;
import com.marginalia.dev.entities.NotificationPreferences;
import com.marginalia.dev.entities.NotificationQueueItem;
import com.marginalia.dev.entities.User;
import com.marginalia.dev.repo.dao.DeviceDao;
import com.marginalia.dev.repo.dao.NotificationDao;
import com.marginalia.dev.repo.dao.NotificationLogDao;
import com.marginalia.dev.repo.dao.NotificationPreferencesDao;
import com.marginalia.dev.repo.dao.NotificationQueueDao;
import com.marginalia.dev.repo.dao.UserDao;
import com.marginalia.dev.services.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Polls notification_queue every 30 seconds, claims due 'pending' rows with
 * SELECT FOR UPDATE SKIP LOCKED, runs final-mile gates, and pushes each to
 * all of the user's devices via FCM. Marks the queue row 'sent' on success,
 * 'suppressed' if a gate blocks it, or 'failed' on send error.
 *
 * Every outcome — sent (per device), suppressed, failed — is also recorded in
 * notification_log for audit/observability. The log table is partitioned by
 * month; {@link PartitionMaintenanceScheduler} keeps next month's partition
 * ready so inserts never break at month boundaries.
 *
 * Final-mile gates (run AT SEND TIME, after the row was enqueued):
 *   1. master switch    — user toggled notifications off
 *   2. per-category     — user disabled this category (ritual/passage/…)
 *   3. frequency cap    — user already received a non-transactional push in the last 24h
 *   4. quiet hours      — current local time falls inside the user's quiet window
 * Transactional categories ('billing', 'auth') bypass gates 3 and 4.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private static final int BATCH_SIZE = 50;

    private static final Set<String> TRANSACTIONAL = Set.of("billing", "auth");

    @Autowired
    private NotificationQueueDao queueDao;

    @Autowired
    private NotificationLogDao logDao;

    @Autowired
    private NotificationPreferencesDao prefsDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private NotificationSender sender;

    private final ObjectMapper mapper = new ObjectMapper();

    @Scheduled(fixedDelay = 30_000L)
    public void run() {
        dispatchBatch();
    }

    @Transactional
    public void dispatchBatch() {
        List<NotificationQueueItem> batch = queueDao.claimDuePending(BATCH_SIZE);
        if (batch.isEmpty()) return;

        log.debug("Dispatching {} notification(s)", batch.size());
        for (NotificationQueueItem item : batch) {
            try {
                deliver(item);
            } catch (Exception e) {
                queueDao.markFailed(item.getId(), e.getMessage());
                logDao.record(item.getUserId(), item.getCategory(), "failed",
                        null, null, item.getPayload());
                log.warn("Notification {} failed: {}", item.getId(), e.getMessage());
            }
        }
    }

    private void deliver(NotificationQueueItem item) throws Exception {
        // ── Final-mile gates ─────────────────────────────────────────────
        String suppression = checkGates(item);
        if (suppression != null) {
            queueDao.markSuppressed(item.getId(), suppression);
            logDao.record(item.getUserId(), item.getCategory(), "suppressed",
                    null, null, item.getPayload());
            log.debug("Notification {} suppressed: {}", item.getId(), suppression);
            return;
        }

        // ── Build payload & deliver to every device the user owns ────────
        JsonNode payload = mapper.readTree(item.getPayload());
        String title = textOrNull(payload, "title");
        String body  = textOrNull(payload, "body");
        Map<String, String> data = readDataMap(payload.get("data"));
        data.put("category", item.getCategory());

        List<Device> devices = deviceDao.findByUser(item.getUserId());
        int sent = 0;
        for (Device d : devices) {
            if (d.getPushToken() == null || d.getPushToken().isBlank()) continue;
            try {
                String fcmId = sender.send(d.getPushToken(), title, body, data);
                sent++;
                // fcmId is null in DRY-RUN — still record the intent for analytics.
                logDao.record(item.getUserId(), item.getCategory(),
                        sender.isDryRun() ? "dry_run" : "sent",
                        fcmId, d.getId(), item.getPayload());
            } catch (Exception sendErr) {
                logDao.record(item.getUserId(), item.getCategory(), "failed",
                        null, d.getId(), item.getPayload());
                log.warn("FCM send to device {} failed: {}", d.getId(), sendErr.getMessage());
            }
        }

        // Persist to user inbox table so it appears in the mobile notifications screen
        try {
            notificationDao.create(item.getUserId(), item.getCategory(), title != null ? title : "Notification",
                    body != null ? body : "", mapper.writeValueAsString(data));
        } catch (Exception e) {
            log.warn("Failed to persist notification to inbox for user {}: {}", item.getUserId(), e.getMessage());
        }

        if (sent == 0 && !sender.isDryRun()) {
            queueDao.markSuppressed(item.getId(), "no eligible devices");
            logDao.record(item.getUserId(), item.getCategory(), "suppressed",
                    null, null, item.getPayload());
            return;
        }
        queueDao.markSent(item.getId());
    }

    /**
     * Returns null when the row may proceed, or a short reason string to
     * record on the suppressed row.
     */
    private String checkGates(NotificationQueueItem item) throws Exception {
        String category = item.getCategory();
        boolean transactional = category != null && TRANSACTIONAL.contains(category);

        NotificationPreferences prefs = prefsDao.findByUserId(item.getUserId()).orElse(null);

        // 1) Master switch — transactional still goes through.
        if (!transactional && prefs != null && Boolean.FALSE.equals(prefs.getEnabled())) {
            return "user disabled notifications";
        }

        // 2) Per-category opt-out — transactional bypasses.
        if (!transactional && prefs != null && prefs.getCategories() != null) {
            JsonNode cats = mapper.readTree(prefs.getCategories());
            JsonNode flag = cats.get(category);
            if (flag != null && flag.isBoolean() && !flag.asBoolean()) {
                return "category '" + category + "' disabled";
            }
        }

        // 3) Frequency cap — at most one non-transactional push per 24h.
        if (!transactional && queueDao.userHasRecentSend(item.getUserId())) {
            return "frequency cap (24h)";
        }

        // 4) Quiet hours — based on the user's local time, not server time.
        if (!transactional && prefs != null
                && prefs.getQuietStartLocal() != null
                && prefs.getQuietEndLocal() != null) {
            User u = userDao.findById(item.getUserId()).orElse(null);
            if (u != null && u.getTimezone() != null && !u.getTimezone().isBlank()) {
                try {
                    int hourLocal = ZonedDateTime.now(ZoneId.of(u.getTimezone())).getHour();
                    if (inQuietWindow(hourLocal,
                                      prefs.getQuietStartLocal(),
                                      prefs.getQuietEndLocal())) {
                        return "quiet hours";
                    }
                } catch (DateTimeException tzErr) {
                    log.warn("Unknown timezone '{}' for user {} — skipping quiet-hours check",
                            u.getTimezone(), u.getId());
                }
            }
        }

        return null;
    }

    /**
     * Inclusive of start, exclusive of end. Supports windows that wrap midnight
     * (e.g. 22 → 8 means "10pm through 7:59am").
     */
    static boolean inQuietWindow(int hour, int start, int end) {
        if (start == end) return false; // empty/disabled window
        return start < end
                ? hour >= start && hour < end
                : hour >= start || hour < end;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Map<String, String> readDataMap(JsonNode dataNode) {
        Map<String, String> out = new HashMap<>();
        if (dataNode != null && dataNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = dataNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getValue() != null && !e.getValue().isNull()) {
                    out.put(e.getKey(), e.getValue().asText());
                }
            }
        }
        return out;
    }
}
