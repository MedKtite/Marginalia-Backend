package com.readsake.dev.services;

import com.readsake.dev.dto.preferences.UserPreferencesUpdateRequest;
import com.readsake.dev.entities.UserPreferences;

import java.util.UUID;

public interface UserPreferencesService {

    /**
     * Returns the user's stored preferences, or a transient defaults object if
     * no row exists yet (no DB write — only returned shape is populated).
     */
    UserPreferences get(UUID userId);

    UserPreferences update(UUID userId, UserPreferencesUpdateRequest req);
}
