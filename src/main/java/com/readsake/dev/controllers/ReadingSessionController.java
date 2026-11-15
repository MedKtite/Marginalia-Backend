package com.readsake.dev.controllers;

import com.readsake.dev.dto.session.ReadingSessionCreateRequest;
import com.readsake.dev.dto.session.ReadingSessionResponse;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.ReadingSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/reading-sessions")
public class ReadingSessionController {

    @Autowired
    private ReadingSessionService sessionService;

    @GetMapping
    public List<ReadingSessionResponse> listRecent(@AuthenticationPrincipal CustomUserDetails me,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return sessionService.listRecent(me.getUserId(), limit).stream()
                .map(ReadingSessionResponse::from)
                .toList();
    }

    @GetMapping("/by-book/{bookId}")
    public List<ReadingSessionResponse> listForBook(@AuthenticationPrincipal CustomUserDetails me,
                                                    @PathVariable UUID bookId) {
        return sessionService.listForBook(bookId, me.getUserId()).stream()
                .map(ReadingSessionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ReadingSessionResponse> create(@AuthenticationPrincipal CustomUserDetails me,
                                                         @Valid @RequestBody ReadingSessionCreateRequest req) {
        var saved = sessionService.create(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReadingSessionResponse.from(saved));
    }
}
