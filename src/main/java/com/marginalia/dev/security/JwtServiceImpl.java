package com.marginalia.dev.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);

    /** Minimum key length for HS256 (RFC requires ≥ hash output = 256 bits = 32 bytes). */
    private static final int MIN_KEY_BYTES = 32;

    /** What we generate when no secret is configured — 48 bytes = 384 bits, comfortably above the floor. */
    private static final int GENERATED_KEY_BYTES = 48;

    private final SecretKey signingKey;

    public JwtServiceImpl(@Value("${jwt.secret:}") String configured,
                          @Value("${jwt.secret-file:.jwt-secret}") String secretFile) {
        String secret = (configured != null && !configured.isBlank())
                ? configured
                : loadOrGenerate(Path.of(secretFile));

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_KEY_BYTES + " bytes for HS256 "
                            + "(got " + bytes.length + "). Either set JWT_SECRET to a longer value "
                            + "or delete the secret file to regenerate.");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Read a persisted secret from disk, or generate + save a fresh one. The
     * file is gitignored so dev restarts preserve token validity.
     *
     * In prod, set JWT_SECRET via the secrets manager and this path is never
     * taken. If something accidentally trips it in prod, the secret survives
     * across pod restarts as long as the disk does — which is wrong for a
     * stateless deployment, hence the warning log.
     */
    private static String loadOrGenerate(Path path) {
        try {
            if (Files.exists(path)) {
                String contents = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!contents.isBlank() && contents.getBytes(StandardCharsets.UTF_8).length >= MIN_KEY_BYTES) {
                    log.info("JWT secret loaded from {} (auto-generated)", path.toAbsolutePath());
                    return contents;
                }
                log.warn("Existing JWT secret file {} is too short, regenerating", path);
            }

            byte[] random = new byte[GENERATED_KEY_BYTES];
            new SecureRandom().nextBytes(random);
            String generated = Base64.getEncoder().encodeToString(random);

            Files.writeString(path, generated, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.warn("Generated new JWT secret at {} — fine for dev. "
                    + "In prod, set JWT_SECRET via your secrets manager.",
                    path.toAbsolutePath());
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read or write JWT secret file " + path
                            + ". Either fix file permissions or set JWT_SECRET explicitly.", e);
        }
    }

    @Override
    public String generateAccessToken(UUID userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + SecurityConstants.ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + SecurityConstants.REFRESH_TOKEN_EXPIRATION_MS))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @Override
    public String extractType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    @Override
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getAccessTokenExpirationMs() {
        return SecurityConstants.ACCESS_TOKEN_EXPIRATION_MS;
    }

    @Override
    public long getRefreshTokenExpirationMs() {
        return SecurityConstants.REFRESH_TOKEN_EXPIRATION_MS;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
