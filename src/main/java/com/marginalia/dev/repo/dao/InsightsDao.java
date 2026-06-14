package com.marginalia.dev.repo.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only DAO for the /me/insights endpoint.
 * All methods scope to a single user.
 */
public interface InsightsDao {

    int countBooksFinishedThisYear(UUID userId);

    int countTotalHighlights(UUID userId);

    /** Sum of duration_sec / 60 across sessions in the last {@code days} days. */
    int minutesReadInLastDays(UUID userId, int days);

    /**
     * Distinct days that have at least one session, most recent first.
     * Used to compute the streak in Java.
     */
    List<LocalDate> findSessionDaysDesc(UUID userId, int lookbackDays);

    /**
     * One row per system tag with (name, color_hex, count). Missing tags appear
     * with count = 0 so the response always has 7 rows.
     */
    List<Map<String, Object>> tagBreakdown(UUID userId);
}
