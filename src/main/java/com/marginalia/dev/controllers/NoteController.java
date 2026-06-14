package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.note.NoteCreateRequest;
import com.marginalia.dev.dto.note.NoteResponse;
import com.marginalia.dev.dto.note.NoteUpdateRequest;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.NoteService;
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
