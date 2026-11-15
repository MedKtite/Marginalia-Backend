package com.readsake.dev.controllers;

import com.readsake.dev.dto.notifications.NotificationResponse;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.NotificationInboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/me/notifications")
public class NotificationInboxController {

    @Autowired
    private NotificationInboxService notificationService;

    @GetMapping
    public List<NotificationResponse> list(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(name = "tab", defaultValue = "all") String tab,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return notificationService.listNotifications(me.getUserId(), tab, limit, offset);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal CustomUserDetails me) {
        long count = notificationService.getUnreadCount(me.getUserId());
        return Map.of("unread_count", count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID id) {
        notificationService.markAsRead(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails me) {
        notificationService.markAllAsRead(me.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID id) {
        notificationService.deleteNotification(id, me.getUserId());
        return ResponseEntity.noContent().build();
    }
}
