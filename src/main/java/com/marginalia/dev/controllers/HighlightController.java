package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.highlight.HighlightCreateRequest;
import com.marginalia.dev.dto.highlight.HighlightResponse;
import com.marginalia.dev.dto.highlight.HighlightUpdateRequest;
import com.marginalia.dev.dto.tag.TagResponse;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.HighlightService;
import com.marginalia.dev.services.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/highlights")
public class HighlightController {

    @Autowired
    private HighlightService highlightService;

    @Autowired
    private TagService tagService;

    @GetMapping
    public List<HighlightResponse> listRecent(@AuthenticationPrincipal CustomUserDetails me,
                                              @RequestParam(defaultValue = "20") int limit) {
        return highlightService.listRecent(me.getUserId(), limit).stream()
                .map(HighlightResponse::from)
                .toList();
    }

    @GetMapping("/by-book/{bookId}")
    public List<HighlightResponse> listForBook(@AuthenticationPrincipal CustomUserDetails me,
                                               @PathVariable UUID bookId) {
        return highlightService.listForBook(bookId, me.getUserId()).stream()
                .map(HighlightResponse::from)
                .toList();
    }

    /** Highlights the user bookmarked into their Saved collection. */
    @GetMapping("/saved")
    public List<HighlightResponse> listSaved(@AuthenticationPrincipal CustomUserDetails me) {
        return highlightService.listSaved(me.getUserId()).stream()
                .map(HighlightResponse::from)
                .toList();
    }

    /** Every highlight carrying this tag (system or the user's custom). */
    @GetMapping("/by-tag/{tagId}")
    public List<HighlightResponse> listForTag(@AuthenticationPrincipal CustomUserDetails me,
                                              @PathVariable UUID tagId) {
        return highlightService.listByTag(tagId, me.getUserId()).stream()
                .map(HighlightResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HighlightResponse getOne(@AuthenticationPrincipal CustomUserDetails me,
                                    @PathVariable UUID id) {
        return HighlightResponse.from(highlightService.getOne(id, me.getUserId()));
    }

    @PostMapping
    public ResponseEntity<HighlightResponse> create(@AuthenticationPrincipal CustomUserDetails me,
                                                    @Valid @RequestBody HighlightCreateRequest req) {
        var saved = highlightService.create(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(HighlightResponse.from(saved));
    }

    @PatchMapping("/{id}")
    public HighlightResponse update(@AuthenticationPrincipal CustomUserDetails me,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody HighlightUpdateRequest req) {
        return HighlightResponse.from(highlightService.update(id, req, me.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID id) {
        highlightService.delete(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Tag attach / detach (M2M) ────────────────────────────────────

    @GetMapping("/{id}/tags")
    public List<TagResponse> listTags(@AuthenticationPrincipal CustomUserDetails me,
                                      @PathVariable UUID id) {
        return tagService.listForHighlight(id, me.getUserId()).stream()
                .map(TagResponse::from).toList();
    }

    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<Void> attachTag(@AuthenticationPrincipal CustomUserDetails me,
                                          @PathVariable UUID id,
                                          @PathVariable UUID tagId) {
        tagService.attachTag(id, tagId, me.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<Void> detachTag(@AuthenticationPrincipal CustomUserDetails me,
                                          @PathVariable UUID id,
                                          @PathVariable UUID tagId) {
        tagService.detachTag(id, tagId, me.getUserId());
        return ResponseEntity.noContent().build();
    }
}
