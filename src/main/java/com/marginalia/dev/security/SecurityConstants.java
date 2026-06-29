package com.marginalia.dev.security;

/**
 * Security constants for JWT authentication.
 * The signing secret itself lives in application.properties under jwt.secret
 * and is injected into JwtServiceImpl — not stored as a static field here.
 */
public final class SecurityConstants {

    /** Access token expiration: 30 minutes. */
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 30L * 60L * 1000L;

    /** Refresh token expiration: 7 days. */
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 7L * 24L * 60L * 60L * 1000L;

    public static final String ACCESS_TOKEN_COOKIE  = "marg_tkn";
    public static final String REFRESH_TOKEN_COOKIE = "marg_refresh";

    public static final String AUTH_HEADER  = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /** Public paths that bypass JWT authentication. */
    public static final String[] AUTH_WHITELIST = {
            "/auth/**",
            "/webhooks/**",      // webhook controllers do their own signature checks
            "/covers",           // public cover-image proxy (Image.network can't send cookies)
            "/actuator/health",
            "/error"
    };

    private SecurityConstants() {}
}
