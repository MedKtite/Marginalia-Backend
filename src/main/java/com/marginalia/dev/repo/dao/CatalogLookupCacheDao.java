package com.marginalia.dev.repo.dao;
import java.time.Duration;
import java.util.Optional;
public interface CatalogLookupCacheDao {
    /** A fresh cache row; {@code json} is null when the row records a miss. */
    record Entry(String json) {}
    Optional<Entry> findFresh(String lookupKey);
    void upsert(String lookupKey, String json, Duration ttl);
}
