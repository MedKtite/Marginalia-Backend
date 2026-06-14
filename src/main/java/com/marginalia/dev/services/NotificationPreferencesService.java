package com.marginalia.dev.services;

import com.marginalia.dev.dto.notifications.NotificationPreferencesUpdateRequest;
import com.marginalia.dev.entities.NotificationPreferences;

import java.util.UUID;

public interface NotificationPreferencesService {

    /** Returns stored prefs OR a transient defaults object (no DB write). */
    NotificationPreferences get(UUID userId);

    NotificationPreferences update(UUID userId, NotificationPreferencesUpdateRequest req);
}
