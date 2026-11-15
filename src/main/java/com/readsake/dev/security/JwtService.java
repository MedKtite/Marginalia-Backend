package com.readsake.dev.security;

import java.util.UUID;

public interface JwtService {

    String generateAccessToken(UUID userId, String email);

    String generateRefreshToken(UUID userId);

    UUID extractUserId(String token);

    String extractEmail(String token);

    /** Returns the "type" claim — "access" or "refresh". Throws on invalid/expired. */
    String extractType(String token);

    boolean isValid(String token);

    long getAccessTokenExpirationMs();

    long getRefreshTokenExpirationMs();
}
