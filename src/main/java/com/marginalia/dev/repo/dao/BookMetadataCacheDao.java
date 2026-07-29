package com.marginalia.dev.repo.dao;
import com.marginalia.dev.entities.BookMetadataCache;
import java.util.Optional;
public interface BookMetadataCacheDao {
    Optional<BookMetadataCache> findFresh(String isbn13);
    void upsert(BookMetadataCache cache);
}
