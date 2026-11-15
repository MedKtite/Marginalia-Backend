package com.readsake.dev.services;

import com.readsake.dev.dto.notifications.NotificationPreferencesUpdateRequest;
import com.readsake.dev.entities.NotificationPreferences;

import java.util.UUID;

public interface NotificationPreferencesService {

    /** Returns stored prefs OR a transient defaults object (no DB write). */
    NotificationPreferences get(UUID userId);

    NotificationPreferences update(UUID userId, NotificationPreferencesUpdateRequest req);
}
