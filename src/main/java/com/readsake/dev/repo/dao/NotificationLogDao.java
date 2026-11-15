package com.readsake.dev.repo.dao;

import java.util.UUID;

public interface NotificationLogDao {

    /**
     * Append-only audit row for one notification dispatch outcome.
     * status: 'sent' | 'suppressed' | 'failed' | 'dry_run'
     * fcmMessageId / deviceId may be null for suppressed and pre-device failures.
     */
    void record(UUID userId,
                String category,
                String status,
                String fcmMessageId,
                UUID deviceId,
                String payloadJson);
}
