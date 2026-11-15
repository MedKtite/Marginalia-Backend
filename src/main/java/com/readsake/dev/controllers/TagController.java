package com.readsake.dev.controllers;

import com.readsake.dev.dto.tag.TagCreateRequest;
import com.readsake.dev.dto.tag.TagResponse;
import com.readsake.dev.dto.tag.TagUpdateRequest;
import com.readsake.dev.repo.dao.TagDao;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TagController {

    @Autowired
    private TagDao tagDao;

    @Autowired
    private TagService tagService;

    /**
     * Returns the 7 system tags + the calling user's custom tags.
     * Public (unauth callers see system tags only).
     */
    @GetMapping("/tags")
    public List<TagResponse> listPublic(@AuthenticationPrincipal CustomUserDetails me) {
        var tags = (me == null)
                ? tagDao.findSystemTags()
                : tagDao.findVisibleForUser(me.getUserId());
        return tags.stream().map(TagResponse::from).toList();
    }

    // ─────────────────────────────────────────────────────────────
    // Pro-only custom tag CRUD
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/me/tags")
    public List<TagResponse> listMine(@AuthenticationPrincipal CustomUserDetails me) {
        return tagService.listForUser(me.getUserId()).stream()
                .map(TagResponse::from).toList();
    }

    /**
     * Highlight count per tag for the calling user, keyed by tag id.
     * Tags with zero highlights are simply absent.
     */
    @GetMapping("/me/tags/counts")
    public java.util.Map<UUID, Integer> counts(@AuthenticationPrincipal CustomUserDetails me) {
        return tagDao.countHighlightsByTag(me.getUserId());
    }

    @PostMapping("/me/tags")
    public ResponseEntity<TagResponse> create(@AuthenticationPrincipal CustomUserDetails me,
                                              @Valid @RequestBody TagCreateRequest req) {
        var t = tagService.createCustom(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(t));
    }

    @PatchMapping("/me/tags/{id}")
    public TagResponse update(@AuthenticationPrincipal CustomUserDetails me,
                              @PathVariable UUID id,
                              @Valid @RequestBody TagUpdateRequest req) {
        return TagResponse.from(tagService.updateCustom(id, req, me.getUserId()));
    }

    @DeleteMapping("/me/tags/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID id) {
        tagService.deleteCustom(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }
}
