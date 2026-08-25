package com.marginalia.dev.repo.dao;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenDao {

    void replaceForUser(UUID userId, String tokenHash, OffsetDateTime expiresAt);

    Optional<UUID> consume(String tokenHash);
}
