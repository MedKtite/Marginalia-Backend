package com.marginalia.dev.services;

import com.marginalia.dev.dto.book.BookCreateRequest;
import com.marginalia.dev.dto.book.BookUpdateRequest;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.exceptions.BadRequestException;
import com.marginalia.dev.exceptions.ForbiddenException;
import com.marginalia.dev.exceptions.NotFoundException;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.IngestionJobDao;
import com.marginalia.dev.repo.dao.SubscriptionDao;
import com.marginalia.dev.services.metadata.MetadataResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {

    /** Free-tier book cap. */
    private static final int FREE_TIER_BOOK_LIMIT = 3;

    @Autowired
    private BookDao bookDao;

    @Autowired
    private IngestionJobDao ingestionJobDao;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private MetadataResolver metadataResolver;

    /** Null when S3 isn't configured — in that case file_key is just stored as-is, no verify. */
    @Autowired(required = false)
    private FileStorageService storage;


    @Override
    public List<Book> listForUser(UUID userId, String status) {
        return bookDao.findByUser(userId, status);
    }

    @Override
    public Book getOne(UUID id, UUID userId) {
        return bookDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    @Override
    @Transactional
    public Book create(BookCreateRequest req, UUID userId) {
        //! Free-tier limit check.
        if (!subscriptionDao.isPro(userId)) {
            int count = bookDao.countByUser(userId);
            if (count >= FREE_TIER_BOOK_LIMIT) {
                throw new ForbiddenException(
                        "Free tier book limit reached (" + FREE_TIER_BOOK_LIMIT + " books). " +
                        "Upgrade to Pro for unlimited.");
            }
        }

        Book b = new Book();
        b.setUserId(userId);
        b.setTitle(req.getTitle());
        b.setAuthor(req.getAuthor());
        b.setNarrator(req.getNarrator());
        b.setPublisher(req.getPublisher());
        b.setPublishedYear(req.getPublishedYear());
        b.setIsbn13(req.getIsbn13());
        b.setGoogleId(req.getGoogleId());
        b.setPageCount(req.getPageCount());
        b.setDurationSec(req.getDurationSec());
        b.setLanguage(req.getLanguage());
        b.setFormat(req.getFormat());
        b.setCoverUrl(req.getCoverUrl());
        b.setCoverDominantColor(req.getCoverDominantColor());
        b.setStatus(req.getStatus() != null ? req.getStatus() : defaultStatusFor(req.getFormat()));

        // ── File-key handling ────────────────────────────────────────────
        if (req.getFileKey() != null && !req.getFileKey().isBlank()) {
            if ("physical".equals(req.getFormat())) {
                throw new BadRequestException("physical books cannot have a file_key");
            }
            // Per-user prefix lock — prevents a user from claiming another user's upload.
            String expectedPrefix = "users/" + userId + "/";
            if (!req.getFileKey().startsWith(expectedPrefix)) {
                throw new ForbiddenException("file_key does not belong to the current user");
            }
            if (storage != null) {
                Optional<Long> size = storage.headObject(req.getFileKey());
                if (size.isEmpty()) {
                    throw new BadRequestException(
                            "Upload not found at file_key — did you complete the PUT?");
                }
                b.setFileSizeBytes(size.get());
            }
            b.setFileKey(req.getFileKey());
        } else if (!"physical".equals(req.getFormat())) {
            throw new BadRequestException(
                    "file_key is required for non-physical formats. "
                    + "Call POST /me/uploads/presign first.");
        }

        boolean needsDocumentProcessing = b.getFileKey() != null
                && ("pdf".equals(b.getFormat()) || "epub".equals(b.getFormat()));
        if (needsDocumentProcessing) b.setProcessingStatus("pending");
        Book created = bookDao.insert(b);
        if (needsDocumentProcessing) {
            ingestionJobDao.insert(created.getId(), "UPLOAD_DOCUMENT", created.getFileKey(),
                    java.util.Map.of("format", created.getFormat()));
        }
        return metadataResolver.enrich(created);
    }

    @Override
    @Transactional
    public Book update(UUID id, BookUpdateRequest req, UUID userId) {
        boolean touchLastOpened = req.getStatus() != null
                || req.getProgressPct() != null
                || req.getCursor() != null;

        return bookDao.patch(id, userId,
                        req.getStatus(),
                        req.getProgressPct(),
                        req.getCursor(),
                        touchLastOpened)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        // Read the row first so we know the file_key to clean up after the DELETE commits.
        Book existing = bookDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!bookDao.delete(id, userId)) {
            throw new NotFoundException("Book not found");
        }

        // Best-effort S3 cleanup — runs outside the DB transaction so a storage
        // hiccup doesn't roll back the row deletion the user already saw succeed.
        if (storage != null && existing.getFileKey() != null && !existing.getFileKey().isBlank()) {
            storage.deleteObject(existing.getFileKey());
            if (existing.getReadingFileKey() != null && !existing.getReadingFileKey().isBlank()) {
                storage.deleteObject(existing.getReadingFileKey());
            }
        }
    }

    @Override
    public FileStorageService.PresignedDownload presignDownload(UUID bookId, UUID userId) {
        Book b = bookDao.findByIdAndUser(bookId, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        if (b.getFileKey() == null || b.getFileKey().isBlank()) {
            throw new NotFoundException("This book has no downloadable file (physical entry).");
        }
        if (storage == null) {
            throw new BadRequestException("File storage is not configured on this server.");
        }
        return storage.presignDownload(b.getFileKey());
    }

    @Override
    public BookService.ReadingDownload presignReadingDownload(UUID bookId, UUID userId) {
        Book b = bookDao.findByIdAndUser(bookId, userId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        if (b.getFileKey() == null || b.getFileKey().isBlank() || storage == null) {
            throw new NotFoundException("This book has no readable file.");
        }
        if (b.getReadingFileKey() != null && !b.getReadingFileKey().isBlank()
                && storage.headObject(b.getReadingFileKey()).isPresent()) {
            return new BookService.ReadingDownload(
                    storage.presignDownload(b.getReadingFileKey()), b.getReadingFileKey().endsWith("reader-v1.zip") ? "reader-v1" : "epub");
        }
        return new BookService.ReadingDownload(storage.presignDownload(b.getFileKey()), b.getFormat());
    }

    private String defaultStatusFor(String format) {
        if (format == null) return "reading";
        return switch (format) {
            case "m4b", "mp3" -> "listening";
            default            -> "reading";
        };
    }
}
