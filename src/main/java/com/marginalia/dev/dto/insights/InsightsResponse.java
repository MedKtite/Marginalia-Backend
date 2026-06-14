package com.marginalia.dev.dto.insights;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The shape the Figma Insights tab consumes:
 *   - 4 summary cards (books / highlights / minutes / streak)
 *   - 7-row tag breakdown bar chart
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InsightsResponse {

    private Summary summary;
    private List<TagBreakdownEntry> tagBreakdown;

    public static InsightsResponse of(Summary summary, List<TagBreakdownEntry> tagBreakdown) {
        InsightsResponse r = new InsightsResponse();
        r.summary = summary;
        r.tagBreakdown = tagBreakdown;
        return r;
    }

    public Summary getSummary() { return summary; }
    public List<TagBreakdownEntry> getTagBreakdown() { return tagBreakdown; }

    public static class Summary {
        private final int booksReadThisYear;
        private final int highlightsCount;
        private final int minutesReadThisWeek;
        private final int currentStreakDays;

        public Summary(int booksReadThisYear, int highlightsCount,
                       int minutesReadThisWeek, int currentStreakDays) {
            this.booksReadThisYear   = booksReadThisYear;
            this.highlightsCount     = highlightsCount;
            this.minutesReadThisWeek = minutesReadThisWeek;
            this.currentStreakDays   = currentStreakDays;
        }

        public int getBooksReadThisYear()   { return booksReadThisYear; }
        public int getHighlightsCount()     { return highlightsCount; }
        public int getMinutesReadThisWeek() { return minutesReadThisWeek; }
        public int getCurrentStreakDays()   { return currentStreakDays; }
    }

    public static class TagBreakdownEntry {
        private final String name;
        private final String colorHex;
        private final int count;

        public TagBreakdownEntry(String name, String colorHex, int count) {
            this.name = name;
            this.colorHex = colorHex;
            this.count = count;
        }

        public String getName()     { return name; }
        public String getColorHex() { return colorHex; }
        public int getCount()       { return count; }
    }
}
