package com.readsake.dev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readsake.dev.dto.catalog.CatalogSearchResponse;
import com.readsake.dev.dto.catalog.CatalogVolumeResponse;
import com.readsake.dev.dto.catalog.LibrivoxBookResponse;
import com.readsake.dev.dto.catalog.TopAuthorResponse;
import com.readsake.dev.repo.dao.CatalogLookupCacheDao;
import com.readsake.dev.repo.dao.GutenbergCatalogDao;
import com.readsake.dev.repo.dao.SearchQueryDao;
import com.readsake.dev.services.metadata.GutenbergRdfClient;
import com.readsake.dev.services.metadata.MetadataResolver;
import com.readsake.dev.services.metadata.MetadataResult;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // One attempt only: search must not wait on retries of an optional layer.
    private static final int GOOGLE_MAX_ATTEMPTS = 1;
    private static final Duration GOOGLE_OUTAGE_COOLDOWN = Duration.ofMinutes(2);
    private static final Duration LOOKUP_HIT_TTL = Duration.ofDays(30);
    private static final Duration LOOKUP_MISS_TTL = Duration.ofHours(6);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int CACHE_MAX_ENTRIES = 500;
    private static final int GUTENBERG_MAX = 10;

    @Value("${google.books.api-key:}")
    private String apiKey;

    @Autowired
    private GutenbergCatalogDao gutenbergDao;
    @Autowired
    private com.readsake.dev.repo.dao.IngestionJobDao ingestionJobs;

    @Autowired
    private SearchQueryDao searchQueryDao;

    @Autowired
    private MetadataResolver metadataResolver;

    @Autowired
    private GutenbergRdfClient gutenbergRdfClient;

    @Autowired
    private CatalogLookupCacheDao lookupCacheDao;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private record CachedGoogle(List<CatalogVolumeResponse> items, Instant fetchedAt) {
        boolean fresh() { return fetchedAt.isAfter(Instant.now().minus(CACHE_TTL)); }
    }

    private final ConcurrentHashMap<String, CachedGoogle> googleCache = new ConcurrentHashMap<>();
    private volatile Instant googleUnavailableUntil = Instant.MIN;

    public CatalogSearchResponse search(String q, int limit) {
        if (q == null || q.isBlank()) {
            return CatalogSearchResponse.of(q, false, List.of());
        }
        final String query = q.trim();
        final int max = Math.min(Math.max(limit, 1), 40);

        // Layer 1 — readable-in-app titles from our own index. The first
        // GUTENBERG_MAX lead; the rest back-fill whatever Google can't supply.
        final List<CatalogVolumeResponse> local = gutenbergHits(query, max);
        final List<CatalogVolumeResponse> items =
                new ArrayList<>(local.subList(0, Math.min(local.size(), GUTENBERG_MAX)));

        // Layer 2 — Google Books, degraded gracefully.
        boolean partial = false;
        List<CatalogVolumeResponse> google = List.of();
        try {
            google = googleHits(query, max);
        } catch (Exception e) {
            // Timeouts count as an outage too, so the next searches skip Google.
            googleUnavailableUntil = Instant.now().plus(GOOGLE_OUTAGE_COOLDOWN);
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
        for (final CatalogVolumeResponse g : local.subList(Math.min(local.size(), GUTENBERG_MAX), local.size())) {
            if (items.size() >= max) break;
            if (seen.add(dedupeKey(g.getTitle(), g.getAuthor()))) items.add(g);
        }
        var response = CatalogSearchResponse.of(query, partial, items);
        if (ingestionJobs.gutenbergUnavailable()) {
            response.setPartialResults(true);
            response.setMessage("The book provider is temporarily unavailable. You can browse results, but new book downloads may be delayed.");
        } else if (partial) {
            response.setMessage("The search provider is temporarily unavailable. Showing available catalog results. Please try again later.");
        }
        return response;
    }

    /**
     * "Recommended for you" — books surfaced from the user's own recent catalog
     * searches. Each recent distinct query is re-run against the (24h-cached)
     * Google layer, results are deduped across queries and marked FULL when the
     * Gutenberg index has the title. Empty history falls back to broad starter
     * interests selected from the request locale. Interleaved by query so one
     * keyword can't monopolize.
     */
    /** Local-only Home feed; never calls Gutendex or Google Books. */
    public List<CatalogVolumeResponse> trending(int max) {
        final List<CatalogVolumeResponse> out = new ArrayList<>();
        for (final Map<String, Object> row : gutenbergDao.trending(Math.min(Math.max(max, 1), 24))) {
            out.add(fromGutenberg(row));
        }
        return out;
    }

    /** Precomputed nightly author ranking; no external service calls. */
    public List<TopAuthorResponse> topAuthors(int max) {
        final List<TopAuthorResponse> out = new ArrayList<>();
        for (final Map<String, Object> row
                : gutenbergDao.topAuthors(Math.min(Math.max(max, 1), 24))) {
            final TopAuthorResponse author = new TopAuthorResponse();
            author.setName((String) row.get("author_name"));
            author.setRank(((Number) row.get("rank")).intValue());
            author.setDownloadCount(((Number) row.get("download_count")).longValue());
            author.setBookCount(((Number) row.get("book_count")).intValue());
            author.setImageUrl((String) row.get("image_url"));
            author.setTagline((String) row.get("tagline"));
            out.add(author);
        }
        return out;
    }

    public List<CatalogVolumeResponse> recommendations(UUID userId, int max, String acceptLanguage) {
        final List<String> recent = searchQueryDao.recentDistinct(userId, 5);
        final List<String> seeds = recent.isEmpty()
                ? starterQueries(userId, acceptLanguage)
                : recent;

        // Home must remain independent of external catalog availability.
        // Re-run personalization seeds against the locally synced Gutenberg
        // index; explicit Discovery searches can still use Google Books.
        final List<List<CatalogVolumeResponse>> perQuery = new ArrayList<>();
        for (final String q : seeds) {
            try {
                perQuery.add(gutenbergHits(q, 8));
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

    private List<String> starterQueries(UUID userId, String acceptLanguage) {
        final List<String> pool = starterPool(acceptLanguage);
        final int daySeed = LocalDate.now(ZoneOffset.UTC).getDayOfYear();
        final int start = Math.floorMod(userId.hashCode() + daySeed, pool.size());
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(5, pool.size()); i++) {
            out.add(pool.get((start + i) % pool.size()));
        }
        return out;
    }

    private static List<String> starterPool(String acceptLanguage) {
        final String lang = acceptLanguage == null ? "" : acceptLanguage.toLowerCase();
        if (lang.startsWith("fr")) {
            return List.of("french literature", "philosophy", "classics", "poetry", "history", "essays");
        }
        if (lang.startsWith("ar")) {
            return List.of("arabic literature", "philosophy", "poetry", "history", "classics", "biography");
        }
        if (lang.startsWith("es")) {
            return List.of("spanish literature", "magical realism", "classics", "history", "poetry", "fiction");
        }
        return List.of("classics", "fiction", "self help", "history", "biography", "philosophy", "poetry");
    }

    /**
     * Book-page details (description, pages, year, downloads). Resolved through
     * the provider chain once, then served from catalog_lookup_cache so every
     * later open is instant and identical. Gutenberg titles also read the
     * gutenberg.org record by id, whose summary is preferred over title-matched
     * descriptions.
     */
    public CatalogVolumeResponse lookup(String title, String author, Integer gutenbergId) {
        final boolean hasTitle = title != null && !title.isBlank();
        if (!hasTitle && gutenbergId == null) return null;
        final String key = gutenbergId != null
                ? "gutenberg:" + gutenbergId
                : "title:" + TextNormalizer.normalize(title) + "|" + TextNormalizer.tokenKey(author);

        final var cached = lookupCacheDao.findFresh(key);
        if (cached.isPresent()) {
            final String json = cached.get().json();
            if (json == null) return null;
            try {
                return mapper.readValue(json, CatalogVolumeResponse.class);
            } catch (Exception e) {
                log.warn("Unreadable lookup cache entry {} — refreshing ({})", key, e.getMessage());
            }
        }

        final var byId = gutenbergId == null
                ? java.util.concurrent.CompletableFuture.completedFuture(Optional.<MetadataResult>empty())
                : java.util.concurrent.CompletableFuture.supplyAsync(() -> gutenbergRdfClient.findById(gutenbergId));
        final Optional<MetadataResult> resolved = hasTitle
                ? metadataResolver.resolve(null, title.trim(), author)
                : Optional.empty();
        final Optional<MetadataResult> gutenberg = byId.join();

        final CatalogVolumeResponse out = resolved.map(this::fromMetadata)
                .or(() -> gutenberg.map(this::fromMetadata))
                .orElse(null);
        if (out != null && gutenberg.isPresent()) {
            if (gutenberg.get().description() != null) {
                out.setDescription(cleanGeneratedSummary(gutenberg.get().description()));
            }
            out.setDownloadCount(downloadCount(gutenberg.get().raw()));
        }

        // Cache a complete answer for long; a miss or a description-less answer
        // (often a provider timeout) only briefly, so it is retried later.
        final boolean complete = out != null && out.getDescription() != null;
        try {
            lookupCacheDao.upsert(key, out == null ? null : mapper.writeValueAsString(out),
                    complete ? LOOKUP_HIT_TTL : LOOKUP_MISS_TTL);
        } catch (Exception e) {
            log.warn("Could not cache lookup {} ({})", key, e.getMessage());
        }
        return out;
    }

    /** Gutenberg appends a provenance note to generated summaries. */
    private static String cleanGeneratedSummary(String summary) {
        return summary.replaceAll("\\(This is an automatically generated summary\\.?\\)\\s*$", "").trim();
    }

    private Integer downloadCount(String rawJson) {
        try {
            final JsonNode count = mapper.readTree(rawJson).path("download_count");
            return count.isNumber() ? count.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private CatalogVolumeResponse fromMetadata(MetadataResult metadata) {
        final CatalogVolumeResponse out = new CatalogVolumeResponse();
        out.setSource(switch (metadata.source()) {
            case "google_books" -> CatalogVolumeResponse.SOURCE_GOOGLE;
            case "open_library" -> CatalogVolumeResponse.SOURCE_OPEN_LIBRARY;
            case "gutendex" -> "GUTENDEX";
            case "wikidata" -> "WIKIDATA";
            default -> metadata.source().toUpperCase(java.util.Locale.ROOT);
        });
        out.setContentAvailability(CatalogVolumeResponse.AVAILABILITY_METADATA_ONLY);
        out.setTitle(metadata.title());
        out.setAuthor(metadata.authors() == null || metadata.authors().isEmpty()
                ? null : String.join(", ", metadata.authors()));
        out.setPublisher(metadata.publisher());
        out.setPublishedYear(metadata.publishedYear());
        out.setPageCount(metadata.pageCount());
        out.setIsbn13(metadata.isbn13());
        out.setThumbnailUrl(metadata.coverUrl());
        out.setDescription(metadata.description());
        markAvailability(out);
        return out;
    }

    /**
     * Fetches a Gutenberg EPUB server-side for the web build (gutenberg.org
     * sends no CORS headers, so browsers can't download it directly). Prefers
     * the lightweight no-images build; validates the ZIP magic so an HTML
     * interstitial never masquerades as a book. Null when nothing worked.
     */
    public byte[] fetchGutenbergEpub(int gutenbergId) {
        final String[] candidates = {
                "https://gutenberg.pglaf.org/cache/epub/" + gutenbergId + "/pg" + gutenbergId + ".epub",
                "https://gutenberg.pglaf.org/cache/epub/" + gutenbergId + "/pg" + gutenbergId + "-images.epub",
                "https://www.gutenberg.org/ebooks/" + gutenbergId + ".epub.noimages",
                "https://www.gutenberg.org/ebooks/" + gutenbergId + ".epub.images",
        };
        final HttpClient redirecting = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (final String url : candidates) {
            try {
                final HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();
                final HttpResponse<byte[]> resp =
                        redirecting.send(req, HttpResponse.BodyHandlers.ofByteArray());
                final byte[] body = resp.body();
                if (resp.statusCode() == 200 && body != null && body.length > 2
                        && body[0] == 0x50 && body[1] == 0x4B) { // "PK" — a ZIP
                    return body;
                }
            } catch (Exception e) {
                log.warn("Gutenberg EPUB fetch failed for {} ({})", url, e.getMessage());
            }
        }
        return null;
    }

    // ── LibriVox: free public-domain audiobooks ─────────────────────────────

    private record CachedLibrivox(LibrivoxBookResponse book, Instant fetchedAt) {
        boolean fresh() { return fetchedAt.isAfter(Instant.now().minus(CACHE_TTL)); }
    }

    /** Caches misses too (book == null) — retrying LibriVox hourly is pointless. */
    private final ConcurrentHashMap<String, CachedLibrivox> librivoxCache =
            new ConcurrentHashMap<>();

    /**
     * Best LibriVox recording for a title/author — free, legal audio for
     * public-domain books (the audio sibling of the Gutenberg reading flow).
     * Sections are chapter MP3s on the Internet Archive. Null when nothing
     * matches (most in-copyright titles).
     */
    public LibrivoxBookResponse librivoxLookup(String title, String author) {
        if (title == null || title.isBlank()) return null;
        final String key = TextNormalizer.normalize(title) + "|"
                + TextNormalizer.tokenKey(author);
        final CachedLibrivox cached = librivoxCache.get(key);
        if (cached != null && cached.fresh()) return cached.book();

        try {
            final LibrivoxBookResponse found = fetchLibrivox(title.trim(), author);
            // Cache completed lookups only — a genuine "no recording" is worth
            // remembering for 24h, but a timeout must NOT poison the cache.
            if (librivoxCache.size() >= CACHE_MAX_ENTRIES) librivoxCache.clear();
            librivoxCache.put(key, new CachedLibrivox(found, Instant.now()));
            return found;
        } catch (Exception e) {
            log.warn("LibriVox lookup failed for '{}' ({})", title, e.getMessage());
            return null;
        }
    }

    private LibrivoxBookResponse fetchLibrivox(String title, String author) throws Exception {
        // extended=1 responses are heavy and librivox.org is slow (~7s for
        // popular titles) — small limit, generous timeout, and results are
        // cached for 24h so users only ever pay this once per title.
        final String url = "https://librivox.org/api/feed/audiobooks"
                + "?format=json&extended=1&limit=5&title="
                + URLEncoder.encode(title, StandardCharsets.UTF_8);
        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Readsake/0.1")
                .GET()
                .build();
        final HttpResponse<String> resp =
                http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;

        final JsonNode books = mapper.readTree(resp.body()).path("books");
        if (!books.isArray()) return null; // {"error": "..."} on no match

        for (final JsonNode b : books) {
            final String bookAuthors = joinLibrivoxAuthors(b.path("authors"));
            // Title search is fuzzy — require an author-token overlap when we
            // know the author, or a wrong edition sneaks in.
            if (author != null && !author.isBlank()
                    && !TextNormalizer.authorsOverlap(author, bookAuthors)) {
                continue;
            }
            final LibrivoxBookResponse out = mapLibrivox(b, bookAuthors);
            if (!out.getSections().isEmpty()) return out;
        }
        return null;
    }

    private static LibrivoxBookResponse mapLibrivox(JsonNode b, String authors) {
        final LibrivoxBookResponse out = new LibrivoxBookResponse();
        out.setLibrivoxId(b.path("id").asLong());
        out.setTitle(textOrNull(b.path("title")));
        out.setAuthor(authors == null || authors.isBlank() ? null : authors);
        final int total = b.path("totaltimesecs").asInt(0);
        out.setTotalTimeSecs(total > 0 ? total : null);
        for (final JsonNode s : b.path("sections")) {
            final String listen = textOrNull(s.path("listen_url"));
            if (listen == null || listen.isBlank()) continue;
            final LibrivoxBookResponse.Section sec = new LibrivoxBookResponse.Section();
            // Archive.org serves both; https avoids cleartext-traffic blocks.
            sec.setListenUrl(listen.replaceFirst("^http://", "https://"));
            sec.setTitle(textOrNull(s.path("title")));
            sec.setPlaytimeSecs(parsePlaytime(textOrNull(s.path("playtime"))));
            out.getSections().add(sec);
        }
        return out;
    }

    /** LibriVox playtime is usually plain seconds ("1030"), sometimes "H:MM:SS". */
    private static Integer parsePlaytime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            if (!raw.contains(":")) return Integer.parseInt(raw.trim());
            int total = 0;
            for (final String part : raw.trim().split(":")) {
                total = total * 60 + Integer.parseInt(part);
            }
            return total;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String joinLibrivoxAuthors(JsonNode authors) {
        final StringBuilder sb = new StringBuilder();
        for (final JsonNode a : authors) {
            final String name = ((a.path("first_name").asText("") + " "
                    + a.path("last_name").asText("")).trim());
            if (name.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }
        return sb.toString();
    }

    // ── Layer 1: Gutenberg index ────────────────────────────────────────────

    /** Google-style operators the app sends from filters and category chips. */
    private static final Pattern SEARCH_OPERATOR = Pattern.compile(
            "(intitle|inauthor|subject|isbn):(?:\"([^\"]*)\"|(\\S+))", Pattern.CASE_INSENSITIVE);

    /**
     * Discovery categories that are not Library of Congress subject headings
     * (what Gutenberg's subjects use) mapped to headings that are.
     */
    private static final Map<String, List<String>> SUBJECT_ALIASES = Map.of(
            "classics", List.of("Classical literature", "Epic poetry", "Greek literature", "Latin literature"),
            "self help", List.of("Conduct of life", "Success", "Self-culture"),
            "business", List.of("Business", "Commerce", "Economics"),
            "biography", List.of("Biography", "Autobiographies"));

    private List<Map<String, Object>> gutenbergRows(String query, int limit) {
        final Matcher m = SEARCH_OPERATOR.matcher(query);
        if (!m.find()) return gutenbergDao.search(query, limit);

        String title = null;
        final List<String> authors = new ArrayList<>();
        final List<String> subjects = new ArrayList<>();
        boolean isbnOnly = true;
        m.reset();
        while (m.find()) {
            final String value = (m.group(2) != null ? m.group(2) : m.group(3)).trim();
            if (value.isEmpty()) continue;
            switch (m.group(1).toLowerCase(java.util.Locale.ROOT)) {
                case "intitle" -> { title = TextNormalizer.normalize(value); isbnOnly = false; }
                case "inauthor" -> { authors.addAll(List.of(value.split("[\\s,]+"))); isbnOnly = false; }
                case "subject" -> {
                    subjects.addAll(SUBJECT_ALIASES.getOrDefault(
                            value.toLowerCase(java.util.Locale.ROOT), List.of(value)));
                    isbnOnly = false;
                }
                default -> { } // isbn: Gutenberg titles carry no ISBNs
            }
        }
        final String text = SEARCH_OPERATOR.matcher(query).replaceAll(" ").trim();
        if (isbnOnly && text.isEmpty()) return List.of();
        authors.removeIf(String::isBlank);
        return gutenbergDao.advancedSearch(text, title, authors, subjects, limit);
    }

    private List<CatalogVolumeResponse> gutenbergHits(String query, int limit) {
        try {
            final List<CatalogVolumeResponse> out = new ArrayList<>();
            for (final Map<String, Object> row : gutenbergRows(query, limit)) {
                out.add(fromGutenberg(row));
            }
            return out;
        } catch (Exception e) {
            // Table missing / DB hiccup must not take catalog search down.
            log.warn("Gutenberg index lookup failed", e);
            return List.of();
        }
    }

    private CatalogVolumeResponse fromGutenberg(Map<String, Object> row) {
        final int id = ((Number) row.get("gutenberg_id")).intValue();
        final CatalogVolumeResponse r = new CatalogVolumeResponse();
        r.setSource(CatalogVolumeResponse.SOURCE_GUTENBERG);
        r.setContentAvailability(CatalogVolumeResponse.AVAILABILITY_FULL);
        r.setGutenbergId(id);
        r.setTitle((String) row.get("title"));
        r.setAuthor(TextNormalizer.displayAuthor((String) row.get("authors")));
        r.setLanguage((String) row.get("language"));
        r.setThumbnailUrl(gutenbergDao.findReaderCoverKey(id).isPresent()
                ? "/covers/gutenberg/" + id : coverUrlFor(id));
        final Object downloadCount = row.get("download_count");
        if (downloadCount instanceof Number number) {
            r.setDownloadCount(number.intValue());
        }
        return r;
    }

    /**
     * Gutenberg's generated cover, served from PGLAF's mirror (www.gutenberg.org
     * returns 503s under load); the client falls back to a text cover on 404.
     */
    private static String coverUrlFor(int gutenbergId) {
        return "https://gutenberg.pglaf.org/cache/epub/" + gutenbergId
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
        if (Instant.now().isBefore(googleUnavailableUntil)) {
            throw new IllegalStateException("Google Books is temporarily unavailable");
        }
        final StringBuilder url = new StringBuilder("https://www.googleapis.com/books/v1/volumes?q=")
                .append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                .append("&maxResults=").append(max)
                .append("&printType=books");
        if (apiKey != null && !apiKey.isBlank()) {
            url.append("&key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        } else {
            log.warn("google.books.api-key is not set — Google Books will reject the request (429).");
        }

        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(GOOGLE_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> resp = null;
        for (int attempt = 1; attempt <= GOOGLE_MAX_ATTEMPTS; attempt++) {
            resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) break;

            final int status = resp.statusCode();
            final boolean retryable = status == 429 || status == 500 || status == 502
                    || status == 503 || status == 504;
            if (!retryable || attempt == GOOGLE_MAX_ATTEMPTS) {
                if (status == 429 || status == 503) {
                    googleUnavailableUntil = Instant.now().plus(GOOGLE_OUTAGE_COOLDOWN);
                    log.warn("Google Books temporarily unavailable (HTTP {}); suppressing requests for {} seconds",
                            status, GOOGLE_OUTAGE_COOLDOWN.toSeconds());
                    return List.of();
                }
                throw new IllegalStateException("Google Books HTTP " + status);
            }

            long delayMillis = retryDelayMillis(resp, attempt);
            log.warn("Google Books HTTP {}; retrying attempt {}/{} in {} ms",
                    status, attempt + 1, GOOGLE_MAX_ATTEMPTS, delayMillis);
            Thread.sleep(delayMillis);
        }
        if (resp == null || resp.statusCode() != 200) {
            throw new IllegalStateException("Google Books request failed");
        }

        final JsonNode root = mapper.readTree(resp.body());
        final List<CatalogVolumeResponse> out = new ArrayList<>();
        for (final JsonNode item : root.path("items")) {
            final CatalogVolumeResponse v = map(item);
            if (v.getTitle() != null) out.add(v);
        }
        return out;
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        try {
            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
            if (retryAfter != null) return Math.min(2000L, Math.max(100L, Long.parseLong(retryAfter) * 1000L));
        } catch (NumberFormatException ignored) {
            // Fall through to bounded exponential backoff.
        }
        return Math.min(1500L, 250L * (1L << Math.max(0, attempt - 1)));
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
