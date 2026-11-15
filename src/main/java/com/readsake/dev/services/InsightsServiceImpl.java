package com.readsake.dev.services;

import com.readsake.dev.dto.insights.InsightsResponse;
import com.readsake.dev.repo.dao.InsightsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InsightsServiceImpl implements InsightsService {

    /** Streak lookback — enough to cover any realistic streak length without dragging the query. */
    private static final int STREAK_LOOKBACK_DAYS = 60;

    @Autowired
    private InsightsDao insightsDao;

    @Override
    public InsightsResponse forUser(UUID userId) {
        int books      = insightsDao.countBooksFinishedThisYear(userId);
        int highlights = insightsDao.countTotalHighlights(userId);
        int minutes    = insightsDao.minutesReadInLastDays(userId, 7);
        int streak     = computeStreak(insightsDao.findSessionDaysDesc(userId, STREAK_LOOKBACK_DAYS));

        InsightsResponse.Summary summary =
                new InsightsResponse.Summary(books, highlights, minutes, streak);

        List<InsightsResponse.TagBreakdownEntry> breakdown = new ArrayList<>(7);
        for (Map<String, Object> row : insightsDao.tagBreakdown(userId)) {
            breakdown.add(new InsightsResponse.TagBreakdownEntry(
                    (String) row.get("name"),
                    (String) row.get("color_hex"),
                    ((Number) row.get("count")).intValue()));
        }

        return InsightsResponse.of(summary, breakdown);
    }

    /**
     * Counts consecutive days with at least one session, ending today.
     * If the user hasn't read today but DID read yesterday, the streak starts at
     * yesterday — saves the "I haven't opened the app yet today" panic.
     */
    int computeStreak(List<LocalDate> daysDesc) {
        if (daysDesc.isEmpty()) return 0;

        LocalDate today  = LocalDate.now(ZoneOffset.UTC);
        LocalDate cursor;

        LocalDate first = daysDesc.get(0);
        if (first.equals(today) || first.equals(today.minusDays(1))) {
            cursor = first;
        } else {
            return 0;
        }

        int streak = 0;
        for (LocalDate day : daysDesc) {
            if (day.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
