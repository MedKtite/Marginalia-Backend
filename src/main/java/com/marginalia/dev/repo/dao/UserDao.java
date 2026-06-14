package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByOauth(String provider, String subject);

    User insert(User user);

    void updateLastSeen(UUID userId);
}
