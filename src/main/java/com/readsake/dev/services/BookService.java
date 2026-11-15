package com.readsake.dev.services;

import com.readsake.dev.dto.book.BookCreateRequest;
import com.readsake.dev.dto.book.BookUpdateRequest;
import com.readsake.dev.entities.Book;
import com.readsake.dev.services.FileStorageService.PresignedDownload;

import java.util.List;
import java.util.UUID;

public interface BookService {

    List<Book> listForUser(UUID userId, String status);

    Book getOne(UUID id, UUID userId);

    Book create(BookCreateRequest req, UUID userId);

    Book update(UUID id, BookUpdateRequest req, UUID userId);

    void delete(UUID id, UUID userId);

    /**
     * Returns a short-lived signed download URL for the book's underlying file.
     * Throws {@code NotFoundException} if the book is missing, doesn't belong
     * to the user, or has no file (e.g. physical books).
     */
    PresignedDownload presignDownload(UUID bookId, UUID userId);

    /** Prefer a derived reflowable EPUB when one exists. */
    ReadingDownload presignReadingDownload(UUID bookId, UUID userId);

    record ReadingDownload(PresignedDownload download, String format) {}
}
