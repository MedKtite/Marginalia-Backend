package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.NotificationItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDao {

    NotificationItem create(UUID userId, String category, String title, String body, String dataJson);

    Optional<NotificationItem> findById(UUID id, UUID userId);

    List<NotificationItem> findByUser(UUID userId, String tab, int limit, int offset);

    long countUnread(UUID userId);

    boolean markAsRead(UUID id, UUID userId, OffsetDateTime readAt);

    int markAllAsRead(UUID userId, OffsetDateTime readAt);

    boolean delete(UUID id, UUID userId);
}
