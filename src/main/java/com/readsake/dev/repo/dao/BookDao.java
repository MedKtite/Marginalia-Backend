package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Book;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.readsake.dev.services.metadata.MetadataResult;

public interface BookDao {

    Optional<Book> findByIdAndUser(UUID id, UUID userId);

    /** When status is null, returns all of the user's books. */
    List<Book> findByUser(UUID userId, String status);

    int countByUser(UUID userId);

    Book insert(Book book);

    void linkGutenberg(UUID id, UUID userId, int gutenbergId);

    Optional<Book> patchMetadata(UUID id, UUID userId, MetadataResult metadata);

    /**
     * Partial update — any null argument keeps the existing column value.
     * Returns the updated row, or empty if the book wasn't found / not owned.
     */
    Optional<Book> patch(UUID id, UUID userId,
                         String status,
                         BigDecimal progressPct,
                         String cursorJson,
                         boolean touchLastOpened);

    boolean delete(UUID id, UUID userId);
}
