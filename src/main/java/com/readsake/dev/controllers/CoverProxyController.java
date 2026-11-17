package com.readsake.dev.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Streams a book-cover image through the backend so the mobile client can load
 * covers it may not be able to reach directly. Public (in AUTH_WHITELIST) since
 * Image.network can't send the session cookie — locked to known image hosts to
 * avoid being an open proxy.
 *
 * gutenberg.org quirks handled here: it drops clients with the default Java
 * User-Agent, throttles concurrent hits from one IP (a Trending row fires ~12
 * covers at once), and its HTTP/2 with this client is flaky — hence the
 * explicit UA, HTTP/1.1, generous timeouts, and the in-memory cache so each
 * cover hits Gutenberg exactly once per backend run.
 */
@RestController
public class CoverProxyController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.readsake.dev.repo.dao.GutenbergCatalogDao catalog;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.readsake.dev.services.FileStorageService storage;

    @GetMapping("/covers/gutenberg/{id}")
    public ResponseEntity<byte[]> storedCover(@org.springframework.web.bind.annotation.PathVariable int id) {
        var key = catalog.findReaderCoverKey(id);
        if (storage == null || key.isEmpty()) return ResponseEntity.notFound().build();
        try {
            byte[] bytes = storage.readObject(key.get());
            if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
            return ok(new CachedCover(bytes, "image/jpeg"));
        } catch (Exception e) {
            log.warn("Stored Gutenberg cover unavailable id={}", id);
            return ResponseEntity.status(503).build();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(CoverProxyController.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int CACHE_MAX_ENTRIES = 500;

    private record CachedCover(byte[] bytes, String contentType) {}

    private final ConcurrentHashMap<String, CachedCover> cache = new ConcurrentHashMap<>();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @GetMapping("/covers")
    public ResponseEntity<byte[]> cover(@RequestParam String url) {
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        final String host = uri.getHost();
        if (host == null || !isAllowedHost(host)) {
            return ResponseEntity.badRequest().build();
        }

        final CachedCover cached = cache.get(url);
        if (cached != null) {
            return ok(cached);
        }

        try {
            final HttpResponse<byte[]> resp = http.send(
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(5))
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (resp.statusCode() != 200) {
                return ResponseEntity.status(resp.statusCode()).build();
            }

            final String contentType = resp.headers()
                    .firstValue("content-type")
                    .orElse(MediaType.IMAGE_JPEG_VALUE);

            if (cache.size() >= CACHE_MAX_ENTRIES) cache.clear(); // crude but fine
            final CachedCover fresh = new CachedCover(resp.body(), contentType);
            cache.put(url, fresh);
            return ok(fresh);
        } catch (Exception e) {
            log.warn("Cover proxy failed for {} ({})", host, e.toString());
            return ResponseEntity.status(502).build();
        }
    }

    private static ResponseEntity<byte[]> ok(CachedCover cover) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(cover.bytes());
    }

    private static boolean isAllowedHost(String host) {
        return host.equals("covers.openlibrary.org")
                || host.equals("books.google.com")
                || host.endsWith(".google.com")
                || host.endsWith(".googleusercontent.com")
                || host.equals("www.gutenberg.org")
                || host.endsWith(".gutenberg.org")
                || host.equals("gutenberg.pglaf.org")
                || host.equals("upload.wikimedia.org")
                || host.endsWith(".wikimedia.org")
                || host.endsWith(".wikipedia.org");
    }
}
