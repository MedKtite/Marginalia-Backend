package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.book.BookCreateRequest;
import com.marginalia.dev.dto.book.BookResponse;
import com.marginalia.dev.dto.book.BookUpdateRequest;
import com.marginalia.dev.dto.uploads.DownloadUrlResponse;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.BookService;
import com.marginalia.dev.services.FileStorageService.PresignedDownload;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public List<BookResponse> list(@AuthenticationPrincipal CustomUserDetails me,
                                   @RequestParam(required = false) String status) {
        return bookService.listForUser(me.getUserId(), status).stream()
                .map(BookResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BookResponse getOne(@AuthenticationPrincipal CustomUserDetails me,
                               @PathVariable UUID id) {
        return BookResponse.from(bookService.getOne(id, me.getUserId()));
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@AuthenticationPrincipal CustomUserDetails me,
                                               @Valid @RequestBody BookCreateRequest req) {
        Book created = bookService.create(req, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(created));
    }

    @PatchMapping("/{id}")
    public BookResponse update(@AuthenticationPrincipal CustomUserDetails me,
                               @PathVariable UUID id,
                               @Valid @RequestBody BookUpdateRequest req) {
        return BookResponse.from(bookService.update(id, req, me.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID id) {
        bookService.delete(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download-url")
    public DownloadUrlResponse downloadUrl(@AuthenticationPrincipal CustomUserDetails me,
                                           @PathVariable UUID id) {
        PresignedDownload d = bookService.presignDownload(id, me.getUserId());
        return new DownloadUrlResponse(d.url(), d.expiresAt());
    }
}
