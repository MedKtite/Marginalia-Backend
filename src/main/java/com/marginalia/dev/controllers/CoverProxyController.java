package com.marginalia.dev.controllers;

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
import java.util.concurrent.TimeUnit;

/**
 * Streams a book-cover image through the backend so the mobile client can load
 * covers it may not be able to reach directly. Public (in AUTH_WHITELIST) since
 * Image.network can't send the session cookie — locked to Google image hosts to
 * avoid being an open proxy.
 */
@RestController
public class CoverProxyController {

    private static final Logger log = LoggerFactory.getLogger(CoverProxyController.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
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

        try {
            final HttpResponse<byte[]> resp = http.send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (resp.statusCode() != 200) {
                return ResponseEntity.status(resp.statusCode()).build();
            }

            final String contentType = resp.headers()
                    .firstValue("content-type")
                    .orElse(MediaType.IMAGE_JPEG_VALUE);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(resp.body());
        } catch (Exception e) {
            log.warn("Cover proxy failed for {}", host, e);
            return ResponseEntity.status(502).build();
        }
    }

    private static boolean isAllowedHost(String host) {
        return host.equals("books.google.com")
                || host.endsWith(".google.com")
                || host.endsWith(".googleusercontent.com")
                || host.equals("www.gutenberg.org"); // Discovery: Gutenberg covers
    }
}
