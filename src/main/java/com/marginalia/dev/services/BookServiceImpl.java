package com.marginalia.dev.services;

import com.marginalia.dev.dto.book.BookCreateRequest;
import com.marginalia.dev.dto.book.BookUpdateRequest;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.exceptions.BadRequestException;
import com.marginalia.dev.exceptions.ForbiddenException;
import com.marginalia.dev.exceptions.NotFoundException;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.IngestionJobDao;
import com.marginalia.dev.repo.dao.GutenbergCatalogDao;
import com.marginalia.dev.repo.dao.SubscriptionDao;
import com.marginalia.dev.services.metadata.MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    /** Free-tier book cap. */
    private static final int FREE_TIER_BOOK_LIMIT = 3;
    private static final String GUTENBERG_READER_KEY_SUFFIX = "/reader-v4.zip";

    @Autowired
    private BookDao bookDao;

    @Autowired
    private IngestionJobDao ingestionJobDao;

    @Autowired
    private GutenbergCatalogDao gutenbergCatalogDao;

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
        b.setGutenbergId(req.getGutenbergId());
        b.setPageCount(req.getPageCount());
        b.setDurationSec(req.getDurationSec());
        b.setLanguage(req.getLanguage());
        b.setFormat(req.getFormat());
        b.setCoverUrl(req.getCoverUrl());
        b.setCoverDominantColor(req.getCoverDominantColor());
        b.setStatus(req.getStatus() != null ? req.getStatus() : defaultStatusFor(req.getFormat()));

        if ("physical".equals(req.getFormat()) && req.getGutenbergId() != null) {
            gutenbergCatalogDao.findReaderFileKey(req.getGutenbergId()).ifPresent(b::setReadingFileKey);
        }

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
        boolean needsGutenbergProcessing = "physical".equals(b.getFormat())
                && b.getGutenbergId() != null
                && (b.getReadingFileKey() == null || b.getReadingFileKey().isBlank());
        if (needsDocumentProcessing || needsGutenbergProcessing) {
            b.setProcessingStatus("pending");
        }
        Book created = bookDao.insert(b);
        if (needsDocumentProcessing) {
            ingestionJobDao.insert(created.getId(), "UPLOAD_DOCUMENT", created.getFileKey(),
                    java.util.Map.of("format", created.getFormat()));
        }
        if (needsGutenbergProcessing) {
            enqueueGutenbergReader(created);
        }
        try {
            return metadataResolver.enrich(created);
        } catch (RuntimeException ex) {
            // Metadata providers are optional; a manual book must still be saved.
            log.warn("Metadata enrichment failed for book {}: {}", created.getId(), ex.getMessage());
            return created;
        }
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
        if (storage == null) {
            throw new NotFoundException("This book has no readable file.");
        }
        // Legacy catalog rows may predate books.gutenberg_id. Recover and persist
        // the canonical ID before looking up or enqueueing the Python artifact.
        Integer gutenbergId = b.getGutenbergId();
        if (gutenbergId == null && "physical".equals(b.getFormat())) {
            gutenbergId = gutenbergCatalogDao.findGutenbergIdByTitle(b.getTitle()).orElse(null);
            if (gutenbergId != null) {
                b.setGutenbergId(gutenbergId);
                bookDao.linkGutenberg(b.getId(), userId, gutenbergId);
            }
        }

        if (gutenbergId != null) {
            final String catalogReaderKey = gutenbergCatalogDao
                    .findReaderFileKey(gutenbergId)
                    .orElse(null);
            if (catalogReaderKey == null || !catalogReaderKey.endsWith(GUTENBERG_READER_KEY_SUFFIX)) {
                enqueueGutenbergReader(b);
                throw new NotFoundException("Preparing the latest readable edition.");
            }
            log.debug("Reading Gutenberg package book={} gutenbergId={} readerKey={}",
                    b.getId(), gutenbergId, catalogReaderKey);
            return new BookService.ReadingDownload(
                    storage.presignDownload(catalogReaderKey), "reader-v1");
        }

        String readerKey = b.getReadingFileKey();
        boolean hasReading = readerKey != null && !readerKey.isBlank();
        log.debug("Reading download book={} gutenbergId={} format={} readerKey={} fileKey={}",
                b.getId(), b.getGutenbergId(), b.getFormat(), readerKey, b.getFileKey());
        if (hasReading) {
            String fmt = readerKey.endsWith(".zip") ? "reader-v1" : "epub";
            return new BookService.ReadingDownload(storage.presignDownload(readerKey), fmt);
        }
        if (b.getFileKey() == null || b.getFileKey().isBlank()) {
            throw new NotFoundException("This book has no readable file.");
        }
        return new BookService.ReadingDownload(storage.presignDownload(b.getFileKey()), b.getFormat());
    }

    private void enqueueGutenbergReader(Book book) {
        String gutenbergId = String.valueOf(book.getGutenbergId());
        ingestionJobDao.insertIfAbsent(
                book.getId(),
                "GUTENBERG_READER",
                gutenbergId,
                java.util.Map.of("gutenberg_id", book.getGutenbergId()));
    }

    private String defaultStatusFor(String format) {
        if (format == null) return "reading";
        return switch (format) {
            case "m4b", "mp3" -> "listening";
            default            -> "reading";
        };
    }
}
