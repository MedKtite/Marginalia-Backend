package com.readsake.dev.services;

import com.readsake.dev.dto.notifications.NotificationResponse;
import com.readsake.dev.entities.NotificationItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationInboxService {

    NotificationItem createNotification(UUID userId, String category, String title, String body, Map<String, Object> data, boolean sendPush);

    List<NotificationResponse> listNotifications(UUID userId, String tab, int limit, int offset);

    long getUnreadCount(UUID userId);

    void markAsRead(UUID id, UUID userId);

    void markAllAsRead(UUID userId);

    void deleteNotification(UUID id, UUID userId);

    void populateSampleNotificationsIfEmpty(UUID userId);
}
