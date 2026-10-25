package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.catalog.BookRequestCreateRequest;
import com.marginalia.dev.dto.catalog.CatalogSearchResponse;
import com.marginalia.dev.dto.catalog.CatalogVolumeResponse;
import com.marginalia.dev.dto.catalog.LibrivoxBookResponse;
import com.marginalia.dev.dto.catalog.TopAuthorResponse;
import com.marginalia.dev.repo.dao.BookRequestDao;
import com.marginalia.dev.repo.dao.SearchQueryDao;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController



@RequestMapping("/me/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private BookRequestDao bookRequestDao;

    @Autowired
    private SearchQueryDao searchQueryDao;

    @GetMapping("/search")
    public CatalogSearchResponse search(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        // Search history feeds "Recommended for you" — best-effort, never
        // lets a logging hiccup break search itself.
        if (me != null && q != null && !q.isBlank()) {
            try {
                searchQueryDao.insert(me.getUserId(), q.trim());
            } catch (Exception ignored) {
            }
        }
        return catalogService.search(q, limit);
    }

    /** EPUB proxy for the web build — gutenberg.org has no CORS headers. */
    @GetMapping("/gutenberg-epub/{gutenbergId}")
    public ResponseEntity<byte[]> gutenbergEpub(@PathVariable int gutenbergId) {
        final byte[] epub = catalogService.fetchGutenbergEpub(gutenbergId);
        return epub == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok()
                        .header("Content-Type", "application/epub+zip")
                        .body(epub);
    }

    @GetMapping("/lookup")
    public ResponseEntity<CatalogVolumeResponse> lookup(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer gutenbergId) {
        final CatalogVolumeResponse v = catalogService.lookup(title, author, gutenbergId);
        return v == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(v);
    }

    /** Free LibriVox audiobook for a title/author — 204 when none exists. */
    @GetMapping("/librivox")
    public ResponseEntity<LibrivoxBookResponse> librivox(
            @RequestParam String title,
            @RequestParam(required = false) String author) {
        final LibrivoxBookResponse b = catalogService.librivoxLookup(title, author);
        return b == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(b);
    }

    @GetMapping("/trending")
    public List<CatalogVolumeResponse> trending(@RequestParam(defaultValue = "24") int limit) {
        return catalogService.trending(limit);
    }

    @GetMapping("/top-authors")
    public List<TopAuthorResponse> topAuthors(@RequestParam(defaultValue = "12") int limit) {
        return catalogService.topAuthors(limit);
    }

    @GetMapping("/recommendations")
    public List<CatalogVolumeResponse> recommendations(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(defaultValue = "12") int limit) {
        return catalogService.recommendations(me.getUserId(),
                Math.min(Math.max(limit, 1), 24),
                acceptLanguage);
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> requestBook(@AuthenticationPrincipal CustomUserDetails me,
                                            @Valid @RequestBody BookRequestCreateRequest req) {
        bookRequestDao.insert(me.getUserId(), req.getTitle(), req.getAuthor(),
                req.getIsbn13(), req.getGoogleId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
