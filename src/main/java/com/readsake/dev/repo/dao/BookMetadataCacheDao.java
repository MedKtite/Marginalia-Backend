package com.readsake.dev.repo.dao;
import com.readsake.dev.entities.BookMetadataCache;
import java.util.Optional;
public interface BookMetadataCacheDao {
    Optional<BookMetadataCache> findFresh(String isbn13);
    void upsert(BookMetadataCache cache);
}
