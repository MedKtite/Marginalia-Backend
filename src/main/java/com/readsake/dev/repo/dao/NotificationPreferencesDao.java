package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.NotificationPreferences;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesDao {

    Optional<NotificationPreferences> findByUserId(UUID userId);

    NotificationPreferences upsert(NotificationPreferences prefs);
}
