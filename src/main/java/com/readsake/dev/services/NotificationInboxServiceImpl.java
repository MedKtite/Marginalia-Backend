package com.readsake.dev.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readsake.dev.dto.notifications.NotificationResponse;
import com.readsake.dev.entities.Device;
import com.readsake.dev.entities.NotificationItem;
import com.readsake.dev.exceptions.NotFoundException;
import com.readsake.dev.repo.dao.DeviceDao;
import com.readsake.dev.repo.dao.NotificationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationInboxServiceImpl implements NotificationInboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInboxServiceImpl.class);

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private NotificationSender notificationSender;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional
    public NotificationItem createNotification(UUID userId, String category, String title, String body, Map<String, Object> data, boolean sendPush) {
        String dataJson = null;
        try {
            dataJson = data != null ? mapper.writeValueAsString(data) : "{}";
        } catch (Exception e) {
            log.warn("Failed to serialize notification data for user {}: {}", userId, e.getMessage());
            dataJson = "{}";
        }

        NotificationItem item = notificationDao.create(userId, category, title, body, dataJson);

        if (sendPush) {
            Map<String, String> pushData = new HashMap<>();
            if (data != null) {
                data.forEach((k, v) -> { if (v != null) pushData.put(k, v.toString()); });
            }
            pushData.put("notification_id", item.getId().toString());
            pushData.put("category", category);

            List<Device> devices = deviceDao.findByUser(userId);
            for (Device d : devices) {
                if (d.getPushToken() != null && !d.getPushToken().isBlank()) {
                    try {
                        notificationSender.send(d.getPushToken(), title, body, pushData);
                    } catch (Exception e) {
                        log.warn("Failed to push FCM to device {}: {}", d.getId(), e.getMessage());
                    }
                }
            }
        }

        return item;
    }

    @Override
    public List<NotificationResponse> listNotifications(UUID userId, String tab, int limit, int offset) {
        List<NotificationItem> items = notificationDao.findByUser(userId, tab, limit, offset);
        if (items.isEmpty() && offset == 0 && ("all".equalsIgnoreCase(tab) || tab == null)) {
            populateSampleNotificationsIfEmpty(userId);
            items = notificationDao.findByUser(userId, tab, limit, offset);
        }
        return items.stream().map(NotificationResponse::from).toList();
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationDao.countUnread(userId);
    }

    @Override
    public void markAsRead(UUID id, UUID userId) {
        boolean ok = notificationDao.markAsRead(id, userId, OffsetDateTime.now());
        if (!ok) {
            throw new NotFoundException("Notification not found");
        }
    }

    @Override
    public void markAllAsRead(UUID userId) {
        notificationDao.markAllAsRead(userId, OffsetDateTime.now());
    }

    @Override
    public void deleteNotification(UUID id, UUID userId) {
        boolean ok = notificationDao.delete(id, userId);
        if (!ok) {
            throw new NotFoundException("Notification not found");
        }
    }

    @Override
    @Transactional
    public void populateSampleNotificationsIfEmpty(UUID userId) {
        if (notificationDao.findByUser(userId, "all", 1, 0).isEmpty()) {
            createNotification(userId, "reading_reminder", "Reading reminder",
                    "It's a great time to pick up your book.",
                    Map.of("type", "reading_reminder", "target_route", "/reader"), false);

            createNotification(userId, "streak_update", "Streak update",
                    "You're on a 3-day streak. Keep it going!",
                    Map.of("type", "streak_update", "streak_days", "3", "target_route", "/insights"), false);

            createNotification(userId, "quote_saved", "Quote saved",
                    "You saved a new quote from Pride and Prejudice.",
                    Map.of("type", "quote_saved", "target_route", "/margins"), false);

            createNotification(userId, "new_recommendation", "New recommendation",
                    "We think you'll love The Midnight Library.",
                    Map.of("type", "new_recommendation", "target_route", "/library"), false);

            createNotification(userId, "chapter_milestone", "Chapter milestone",
                    "You've finished Chapter 10 of Moby Dick.",
                    Map.of("type", "chapter_milestone", "chapter", "10", "book", "Moby Dick"), false);

            createNotification(userId, "weekly_insights", "Weekly reading insights",
                    "You read for 4h 32m this week. Keep it up!",
                    Map.of("type", "weekly_insights", "target_route", "/insights"), false);

            createNotification(userId, "quote_of_the_day", "Quote of the day",
                    "“So many books, so little time.”",
                    Map.of("type", "quote_of_the_day", "author", "Frank Zappa"), false);

            createNotification(userId, "challenge_progress", "Challenge progress",
                    "You're 60% toward your August reading goal.",
                    Map.of("type", "challenge_progress", "progress_pct", "60", "target_route", "/insights"), false);
        }
    }
}
