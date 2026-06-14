package com.marginalia.dev.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Real FCM sender if a FirebaseMessaging bean is wired; otherwise logs intended
 * pushes (DRY-RUN). Both code paths satisfy the NotificationSender contract,
 * so dispatcher code never branches.
 */
@Service
public class FcmNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationSender.class);

    /** Optional — null when firebase.service-account-path is blank. */
    @Autowired(required = false)
    private FirebaseMessaging messaging;

    @Override
    public String send(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isBlank()) {
            log.debug("Skipped push: blank token");
            return null;
        }
        if (messaging == null) {
            log.info("[DRY-RUN push] token={}…  title=\"{}\"  body=\"{}\"  data={}",
                    token.substring(0, Math.min(8, token.length())), title, body, data);
            return null;
        }
        try {
            Message.Builder b = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null) {
                data.forEach((k, v) -> { if (v != null) b.putData(k, v); });
            }
            return messaging.send(b.build());
        } catch (Exception e) {
            log.warn("FCM send failed for token prefix {}: {}",
                    token.substring(0, Math.min(8, token.length())), e.getMessage());
            throw new RuntimeException("FCM send failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isDryRun() {
        return messaging == null;
    }
}
