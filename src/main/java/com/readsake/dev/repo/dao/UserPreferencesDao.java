package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.UserPreferences;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesDao {

    Optional<UserPreferences> findByUserId(UUID userId);

    UserPreferences upsert(UserPreferences prefs);
}
