package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.UserPreferences;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesDao {

    Optional<UserPreferences> findByUserId(UUID userId);

    UserPreferences upsert(UserPreferences prefs);
}
