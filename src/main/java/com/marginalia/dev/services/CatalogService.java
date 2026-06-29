package com.marginalia.dev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marginalia.dev.dto.catalog.CatalogVolumeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies catalog search to the Google Books API (volumes endpoint). The API
 * key stays server-side; set {@code google.books.api-key} in the active profile
 * — without it Google returns 429 (the anonymous quota is 0).
 */
@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    @Value("${google.books.api-key:}")
    private String apiKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public List<CatalogVolumeResponse> search(String q, int limit) {
        if (q == null || q.isBlank()) return List.of();
        final int max = Math.min(Math.max(limit, 1), 40);

        try {
            final StringBuilder url = new StringBuilder("https://www.googleapis.com/books/v1/volumes?q=")
                    .append(URLEncoder.encode(q.trim(), StandardCharsets.UTF_8))
                    .append("&maxResults=").append(max)
                    .append("&printType=books");
            if (apiKey != null && !apiKey.isBlank()) {
                url.append("&key=").append(apiKey);
            } else {
                log.warn("google.books.api-key is not set — Google Books will reject the request (429).");
            }

            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            final HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Google Books search failed: HTTP {} — {}", resp.statusCode(), abbreviate(resp.body()));
                throw new RuntimeException("Catalog search is unavailable");
            }

            final JsonNode root = mapper.readTree(resp.body());
            final List<CatalogVolumeResponse> out = new ArrayList<>();
            for (final JsonNode item : root.path("items")) {
                final CatalogVolumeResponse v = map(item);
                if (v.getTitle() != null) out.add(v);
            }
            return out;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google Books search error", e);
            throw new RuntimeException("Catalog search is unavailable");
        }
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

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() <= 300 ? s : s.substring(0, 300) + "…";
    }
}
