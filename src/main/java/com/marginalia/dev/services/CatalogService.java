package com.marginalia.dev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marginalia.dev.dto.catalog.CatalogSearchResponse;
import com.marginalia.dev.dto.catalog.CatalogVolumeResponse;
import com.marginalia.dev.repo.dao.GutenbergCatalogDao;
import com.marginalia.dev.repo.dao.SearchQueryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-layer catalog search behind GET /me/catalog/search.
 *
 * Layer 1 — the synced Gutenberg index (gutenberg_catalog): titles here are
 * public domain and readable in-app, so they rank first and are marked FULL.
 *
 * Layer 2 — the Google Books API (key stays server-side): appended after the
 * Gutenberg hits, deduped by normalized title+author, and marked FULL when the
 * same title exists in the Gutenberg index, else METADATA_ONLY.
 *
 * Google responses are cached in-memory for 24h per normalized query, and the
 * call is capped at 3s — on failure the response degrades to the local layer
 * with partialResults=true instead of erroring.
 */
@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private static final Duration GOOGLE_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int CACHE_MAX_ENTRIES = 500;
    private static final int GUTENBERG_MAX = 10;

    @Value("${google.books.api-key:}")
    private String apiKey;

    @Autowired
    private GutenbergCatalogDao gutenbergDao;

    @Autowired
    private SearchQueryDao searchQueryDao;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private record CachedGoogle(List<CatalogVolumeResponse> items, Instant fetchedAt) {
        boolean fresh() { return fetchedAt.isAfter(Instant.now().minus(CACHE_TTL)); }
    }

    private final ConcurrentHashMap<String, CachedGoogle> googleCache = new ConcurrentHashMap<>();

    public CatalogSearchResponse search(String q, int limit) {
        if (q == null || q.isBlank()) {
            return CatalogSearchResponse.of(q, false, List.of());
        }
        final String query = q.trim();
        final int max = Math.min(Math.max(limit, 1), 40);

        // Layer 1 — readable-in-app titles from our own index.
        final List<CatalogVolumeResponse> items =
                new ArrayList<>(gutenbergHits(query, Math.min(max, GUTENBERG_MAX)));

        // Layer 2 — Google Books, degraded gracefully.
        boolean partial = false;
        List<CatalogVolumeResponse> google = List.of();
        try {
            google = googleHits(query, max);
        } catch (Exception e) {
            log.warn("Google Books unavailable — returning local results only ({})", e.getMessage());
            partial = true;
        }

        final Set<String> seen = new HashSet<>();
        for (final CatalogVolumeResponse g : items) {
            seen.add(dedupeKey(g.getTitle(), g.getAuthor()));
        }
        for (final CatalogVolumeResponse g : google) {
            if (items.size() >= max) break;
            if (!seen.add(dedupeKey(g.getTitle(), g.getAuthor()))) continue;
            markAvailability(g);
            items.add(g);
        }
        return CatalogSearchResponse.of(query, partial, items);
    }

    /**
     * "Recommended for you" — books surfaced from the user's own recent catalog
     * searches. Each recent distinct query is re-run against the (24h-cached)
     * Google layer, results are deduped across queries and marked FULL when the
     * Gutenberg index has the title. Empty history → empty list (client hides
     * the section). Interleaved by query so one keyword can't monopolize.
     */
    public List<CatalogVolumeResponse> recommendations(UUID userId, int max) {
        final List<String> recent = searchQueryDao.recentDistinct(userId, 5);
        if (recent.isEmpty()) return List.of();

        // Fetch a few candidates per keyword, tolerating individual failures.
        final List<List<CatalogVolumeResponse>> perQuery = new ArrayList<>();
        for (final String q : recent) {
            try {
                perQuery.add(googleHits(q, 8));
            } catch (Exception e) {
                log.warn("Recommendation lookup failed for '{}' ({})", q, e.getMessage());
            }
        }

        final Set<String> seen = new HashSet<>();
        final List<CatalogVolumeResponse> out = new ArrayList<>();
        // Round-robin across queries: 1st pick of each keyword, then 2nd, …
        for (int i = 0; out.size() < max; i++) {
            boolean any = false;
            for (final List<CatalogVolumeResponse> hits : perQuery) {
                if (i >= hits.size() || out.size() >= max) continue;
                any = true;
                final CatalogVolumeResponse v = hits.get(i);
                if (v.getTitle() == null) continue;
                if (!seen.add(dedupeKey(v.getTitle(), v.getAuthor()))) continue;
                markAvailability(v);
                out.add(v);
            }
            if (!any) break;
        }
        return out;
    }

    /**
     * Best-matching Google edition for a title/author — powers the Pages/Year
     * (and rating fallback) KPIs for Gutenberg books, which carry neither.
     * Runs through the same 24h-cached Google layer; null when nothing matches.
     */
    public CatalogVolumeResponse lookup(String title, String author) {
        if (title == null || title.isBlank()) return null;
        final String strict = "intitle:\"" + title.trim() + "\""
                + (author == null || author.isBlank() ? "" : " inauthor:\"" + author.trim() + "\"");
        final String loose = (title + " " + (author == null ? "" : author)).trim();
        for (final String q : new String[]{strict, loose}) {
            try {
                final List<CatalogVolumeResponse> hits = googleHits(q, 5);
                for (final CatalogVolumeResponse v : hits) {
                    if (v.getPageCount() != null) return v;
                }
                if (!hits.isEmpty()) return hits.get(0);
            } catch (Exception e) {
                log.warn("Edition lookup failed for '{}' ({})", q, e.getMessage());
            }
        }
        return null;
    }

    // ── Layer 1: Gutenberg index ────────────────────────────────────────────

    private List<CatalogVolumeResponse> gutenbergHits(String query, int limit) {
        try {
            final List<CatalogVolumeResponse> out = new ArrayList<>();
            for (final Map<String, Object> row : gutenbergDao.search(query, limit)) {
                out.add(fromGutenberg(row));
            }
            return out;
        } catch (Exception e) {
            // Table missing / DB hiccup must not take catalog search down.
            log.warn("Gutenberg index lookup failed", e);
            return List.of();
        }
    }

    private static CatalogVolumeResponse fromGutenberg(Map<String, Object> row) {
        final int id = ((Number) row.get("gutenberg_id")).intValue();
        final CatalogVolumeResponse r = new CatalogVolumeResponse();
        r.setSource(CatalogVolumeResponse.SOURCE_GUTENBERG);
        r.setContentAvailability(CatalogVolumeResponse.AVAILABILITY_FULL);
        r.setGutenbergId(id);
        r.setTitle((String) row.get("title"));
        r.setAuthor(TextNormalizer.displayAuthor((String) row.get("authors")));
        r.setLanguage((String) row.get("language"));
        r.setThumbnailUrl(coverUrlFor(id));
        return r;
    }

    /** Gutenberg's generated cover; the client falls back to a text cover on 404. */
    private static String coverUrlFor(int gutenbergId) {
        return "https://www.gutenberg.org/cache/epub/" + gutenbergId
                + "/pg" + gutenbergId + ".cover.medium.jpg";
    }

    /**
     * Marks a Google result FULL (and carries the Gutenberg id) when the same
     * title+author exists in the Gutenberg index; METADATA_ONLY otherwise.
     */
    private void markAvailability(CatalogVolumeResponse g) {
        g.setSource(CatalogVolumeResponse.SOURCE_GOOGLE);
        g.setContentAvailability(CatalogVolumeResponse.AVAILABILITY_METADATA_ONLY);
        try {
            for (final Map<String, Object> row
                    : gutenbergDao.findByTitleNorm(TextNormalizer.normalize(g.getTitle()))) {
                if (TextNormalizer.authorsOverlap(g.getAuthor(), (String) row.get("authors"))) {
                    g.setContentAvailability(CatalogVolumeResponse.AVAILABILITY_FULL);
                    g.setGutenbergId(((Number) row.get("gutenberg_id")).intValue());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Gutenberg availability check failed", e);
        }
    }

    private static String dedupeKey(String title, String author) {
        return TextNormalizer.normalize(title) + "|" + TextNormalizer.tokenKey(author);
    }

    // ── Layer 2: Google Books (cached, 3s cap) ──────────────────────────────

    private List<CatalogVolumeResponse> googleHits(String query, int max) throws Exception {
        final String cacheKey = TextNormalizer.normalize(query) + "|" + max;
        final CachedGoogle cached = googleCache.get(cacheKey);
        if (cached != null && cached.fresh()) return cached.items();

        final List<CatalogVolumeResponse> fetched = fetchGoogle(query, max);
        evictIfNeeded();
        googleCache.put(cacheKey, new CachedGoogle(fetched, Instant.now()));
        return fetched;
    }

    private void evictIfNeeded() {
        if (googleCache.size() < CACHE_MAX_ENTRIES) return;
        googleCache.entrySet().removeIf(e -> !e.getValue().fresh());
        while (googleCache.size() >= CACHE_MAX_ENTRIES) {
            googleCache.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().fetchedAt()))
                    .map(Map.Entry::getKey)
                    .ifPresent(googleCache::remove);
        }
    }

    private List<CatalogVolumeResponse> fetchGoogle(String query, int max) throws Exception {
        final StringBuilder url = new StringBuilder("https://www.googleapis.com/books/v1/volumes?q=")
                .append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                .append("&maxResults=").append(max)
                .append("&printType=books");
        if (apiKey != null && !apiKey.isBlank()) {
            url.append("&key=").append(apiKey);
        } else {
            log.warn("google.books.api-key is not set — Google Books will reject the request (429).");
        }

        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(GOOGLE_TIMEOUT)
                .GET()
                .build();

        final HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Google Books HTTP " + resp.statusCode());
        }

        final JsonNode root = mapper.readTree(resp.body());
        final List<CatalogVolumeResponse> out = new ArrayList<>();
        for (final JsonNode item : root.path("items")) {
            final CatalogVolumeResponse v = map(item);
            if (v.getTitle() != null) out.add(v);
        }
        return out;
    }

    private CatalogVolumeResponse map(JsonNode item) {
        final JsonNode vi = item.path("volumeInfo");
        final CatalogVolumeResponse r = new CatalogVolumeResponse();
        r.setGoogleId(textOrNull(item.path("id")));
        r.setTitle(textOrNull(vi.path("title")));

        final JsonNode authors = vi.path("authors");
        if (authors.isArray() && !authors.isEmpty()) {
            final List<String> names = new ArrayList<>();
            authors.forEach(a -> names.add(a.asText()));
            r.setAuthor(String.join(", ", names));
        }

        r.setPublisher(textOrNull(vi.path("publisher")));
        r.setPublishedYear(parseYear(textOrNull(vi.path("publishedDate"))));
        r.setIsbn13(extractIsbn13(vi.path("industryIdentifiers")));
        r.setPageCount(vi.has("pageCount") ? vi.get("pageCount").asInt() : null);
        r.setThumbnailUrl(httpsThumbnail(vi.path("imageLinks")));
        r.setDescription(textOrNull(vi.path("description")));
        r.setAverageRating(vi.has("averageRating") ? vi.get("averageRating").asDouble() : null);
        r.setRatingsCount(vi.has("ratingsCount") ? vi.get("ratingsCount").asInt() : null);
        // Only advertise a preview when Google actually renders pages for it.
        final String viewability = item.path("accessInfo").path("viewability").asText("");
        if ("PARTIAL".equals(viewability) || "ALL_PAGES".equals(viewability)) {
            final String preview = textOrNull(vi.path("previewLink"));
            r.setPreviewUrl(preview == null ? null : preview.replaceFirst("^http://", "https://"));
        }
        return r;
    }

    private static String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static Integer parseYear(String publishedDate) {
        if (publishedDate == null || publishedDate.length() < 4) return null;
        try {
            return Integer.parseInt(publishedDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractIsbn13(JsonNode identifiers) {
        if (!identifiers.isArray()) return null;
        for (final JsonNode id : identifiers) {
            if ("ISBN_13".equals(id.path("type").asText())) {
                return textOrNull(id.path("identifier"));
            }
        }
        return null;
    }

    private static String httpsThumbnail(JsonNode imageLinks) {
        String url = textOrNull(imageLinks.path("thumbnail"));
        if (url == null) url = textOrNull(imageLinks.path("smallThumbnail"));
        return url == null ? null : url.replaceFirst("^http://", "https://");
    }
}
