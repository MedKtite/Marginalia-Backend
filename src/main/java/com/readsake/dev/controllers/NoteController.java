package com.readsake.dev.controllers;

import com.readsake.dev.dto.note.NoteCreateRequest;
import com.readsake.dev.dto.note.NoteResponse;
import com.readsake.dev.dto.note.NoteUpdateRequest;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.NoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;

    /** The user's most-recent notes across all books, newest first. */
    @GetMapping
    public List<NoteResponse> listRecent(@AuthenticationPrincipal CustomUserDetails me,
                                         @RequestParam(defaultValue = "100") int limit) {
        return noteService.listRecent(me.getUserId(), limit).stream()
                .map(NoteResponse::from)
                .toList();
    }

    /** Notes the user bookmarked into their Saved collection. */
    @GetMapping("/saved")
    public List<NoteResponse> listSaved(@AuthenticationPrincipal CustomUserDetails me) {
        return noteService.listSaved(me.getUserId()).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @GetMapping("/by-book/{bookId}")
    public List<NoteResponse> listForBook(@AuthenticationPrincipal CustomUserDetails me,
                                          @PathVariable UUID bookId) {
        return noteService.listForBook(bookId, me.getUserId()).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @GetMapping("/by-highlight/{highlightId}")
    public List<NoteResponse> listForHighlight(@AuthenticationPrincipal CustomUserDetails me,
                                               @PathVariable UUID highlightId) {
        return noteService.listForHighlight(highlightId, me.getUserId()).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public NoteResponse getOne(@AuthenticationPrincipal CustomUserDetails me,
                               @PathVariable UUID id) {
        return NoteResponse.from(noteService.getOne(id, me.getUserId()));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal CustomUserDetails me,
                                               @Valid @RequestBody NoteCreateRequest req) {
        var saved = noteService.create(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(saved));
    }

    @PatchMapping("/{id}")
    public NoteResponse update(@AuthenticationPrincipal CustomUserDetails me,
                               @PathVariable UUID id,
                               @Valid @RequestBody NoteUpdateRequest req) {
        return NoteResponse.from(noteService.update(id, req, me.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID id) {
        noteService.delete(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }
}
