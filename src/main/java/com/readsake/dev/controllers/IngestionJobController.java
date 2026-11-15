package com.readsake.dev.controllers;

import com.readsake.dev.dto.ingestion.IngestionJobCreateRequest;
import com.readsake.dev.dto.ingestion.IngestionJobResponse;
import com.readsake.dev.entities.IngestionJob;
import com.readsake.dev.repo.dao.IngestionJobDao;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/me/books/{bookId}/ingestion-jobs")
public class IngestionJobController {
    @Autowired private IngestionJobDao jobs;
    @Autowired private BookService books;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestionJobResponse create(@AuthenticationPrincipal CustomUserDetails me,
                                       @PathVariable UUID bookId,
                                       @Valid @RequestBody IngestionJobCreateRequest req) {
        var book = books.getOne(bookId, me.getUserId());
        IngestionJob job = jobs.insert(book.getId(), req.jobType(), req.sourceKey(), req.payload());
        return response(job);
    }

    @GetMapping("/{jobId}")
    public IngestionJobResponse get(@AuthenticationPrincipal CustomUserDetails me,
                                    @PathVariable UUID bookId, @PathVariable UUID jobId) {
        books.getOne(bookId, me.getUserId());
        return jobs.findByIdAndBook(jobId, bookId).map(this::response)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found"));
    }

    private IngestionJobResponse response(IngestionJob j) {
        return new IngestionJobResponse(j.getId(), j.getBookId(), j.getJobType(), j.getSourceKey(), j.getStatus(), j.getAttempts(), j.getResult(), j.getErrorMessage(), j.getCreatedAt(), j.getStartedAt(), j.getFinishedAt());
    }
}
