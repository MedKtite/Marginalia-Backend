package com.readsake.dev.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Text normalization shared by the Gutenberg catalog sync and catalog search
 * matching. Both sides of a comparison MUST go through the same function —
 * {@code title_norm} in gutenberg_catalog is produced by {@link #normalize}.
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /** Lowercase, strip accents, collapse anything non-alphanumeric to single spaces. */
    public static String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /**
     * Order-insensitive author/title key for deduping across sources —
     * "Jane Austen" and "Austen, Jane, 1775-1817" both reduce to "austen jane"
     * (date-like tokens dropped).
     */
    public static String tokenKey(String s) {
        final String[] tokens = normalize(s).split(" ");
        final List<String> kept = new ArrayList<>(tokens.length);
        for (final String t : tokens) {
            if (!t.isEmpty() && !t.matches(".*\\d.*")) kept.add(t);
        }
        kept.sort(String::compareTo);
        return String.join(" ", kept);
    }

    /**
     * Human-readable author(s) from the Gutenberg catalog form
     * "Last, First, 1775-1817; Last, First" → "First Last, First Last".
     */
    public static String displayAuthor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        final List<String> out = new ArrayList<>();
        for (final String one : raw.split(";")) {
            final List<String> keep = new ArrayList<>();
            for (final String part : one.trim().split(",")) {
                final String t = part.trim();
                if (!t.isEmpty() && !t.matches(".*\\d.*")) keep.add(t);
            }
            if (keep.isEmpty()) continue;
            out.add(keep.size() >= 2 ? keep.get(1) + " " + keep.get(0) : keep.get(0));
        }
        return out.isEmpty() ? null : String.join(", ", out);
    }

    /** True when the two author strings share at least one name token. */
    public static boolean authorsOverlap(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) return true; // nothing to contradict
        final var ta = new ArrayList<>(Arrays.asList(tokenKey(a).split(" ")));
        ta.retainAll(Arrays.asList(tokenKey(b).split(" ")));
        ta.removeIf(String::isEmpty);
        return !ta.isEmpty();
    }
}
