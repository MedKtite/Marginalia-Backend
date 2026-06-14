package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.NotificationPreferences;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesDao {

    Optional<NotificationPreferences> findByUserId(UUID userId);

    NotificationPreferences upsert(NotificationPreferences prefs);
}
