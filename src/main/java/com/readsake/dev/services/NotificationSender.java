package com.readsake.dev.services;

import java.util.Map;

public interface NotificationSender {

    /**
     * Send a single push to one device token.
     *
     * @param token   the device's FCM registration token
     * @param title   short title (shown bold on the notification)
     * @param body    body text
     * @param data    extra key/value pairs the mobile app receives via the data payload
     * @return  a provider message id when the send succeeds; null in DRY-RUN mode
     * @throws RuntimeException when the provider rejects the send
     */
    String send(String token, String title, String body, Map<String, String> data);

    boolean isDryRun();
}
