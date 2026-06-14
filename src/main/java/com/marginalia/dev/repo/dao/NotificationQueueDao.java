package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.NotificationQueueItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationQueueDao {

    /**
     * Atomically claim a batch of due, pending rows. Uses FOR UPDATE SKIP LOCKED
     * so multiple dispatcher instances don't compete for the same items.
     * MUST be called inside a transaction.
     */
    List<NotificationQueueItem> claimDuePending(int limit);

    NotificationQueueItem enqueue(UUID userId, String category, OffsetDateTime sendAt, String payloadJson);

    void markSent(UUID id);

    void markFailed(UUID id, String error);

    void markSuppressed(UUID id, String reason);

    /** True if this user has had at least one sent (non-transactional) push in the last 24h. */
    boolean userHasRecentSend(UUID userId);
}
