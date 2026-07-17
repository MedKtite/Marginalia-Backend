package com.marginalia.dev.services;

import com.marginalia.dev.dto.auth.ForgotPasswordRequest;
import com.marginalia.dev.dto.auth.LoginRequest;
import com.marginalia.dev.dto.auth.OAuthLoginRequest;
import com.marginalia.dev.dto.auth.RegisterRequest;
import com.marginalia.dev.dto.auth.ResetPasswordRequest;
import com.marginalia.dev.entities.User;
import com.marginalia.dev.exceptions.BadRequestException;
import com.marginalia.dev.exceptions.ConflictException;
import com.marginalia.dev.exceptions.UnauthorizedException;
import com.marginalia.dev.repo.dao.PasswordResetTokenDao;
import com.marginalia.dev.repo.dao.UserDao;
import com.marginalia.dev.security.JwtService;
import com.marginalia.dev.security.SecurityConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordResetTokenDao passwordResetTokenDao;

    @Autowired
    private PasswordResetEmailService passwordResetEmailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleTokenVerifier googleVerifier;

    @Autowired
    private XTokenVerifier xVerifier;

    // ─────────────────────────────────────────────────────────────────────
    // Password
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public User register(RegisterRequest req, HttpServletResponse response) {
        String email = req.getEmail().trim().toLowerCase();
        if (userDao.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        User u = new User();
        u.setEmail(email);
        u.setDisplayName(req.getDisplayName().trim());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setEmailVerified(false);
        u.setTimezone(req.getTimezone() != null ? req.getTimezone() : "UTC");

        try {
            User saved = userDao.insert(u);
            issueTokens(saved, response);
            return saved;
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Email already registered");
        }
    }

    @Override
    public User login(LoginRequest req, HttpServletResponse response) {
        User u = userDao.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Incorrect email or password."));

        if (u.getPasswordHash() == null) {
            throw new UnauthorizedException("This account uses social sign-in");
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect email or password.");
        }

        userDao.updateLastSeen(u.getId());
        issueTokens(u, response);
        return u;
    }

    // ─────────────────────────────────────────────────────────────────────
    // OAuth
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public User loginWithGoogle(OAuthLoginRequest req, HttpServletResponse response) {
        GoogleTokenVerifier.Result r = googleVerifier.verify(req.getToken());
        return upsertOAuthUser("google", r.subject(), r.email(), r.name(),
                req.getTimezone(), response);
    }

    @Override
    public User loginWithX(OAuthLoginRequest req, HttpServletResponse response) {
        XTokenVerifier.Result r = xVerifier.verify(req.getToken());
        return upsertOAuthUser("x", r.subject(), r.email(), r.name(),
                req.getTimezone(), response);
    }

    private User upsertOAuthUser(String provider, String subject, String email, String name,
                                 String timezone, HttpServletResponse response) {
        Optional<User> existing = userDao.findByOauth(provider, subject);
        if (existing.isPresent()) {
            User u = existing.get();
            userDao.updateLastSeen(u.getId());
            issueTokens(u, response);
            response.setHeader("X-Marginalia-Account", "existing");
            return u;
        }

        // Brand-new social account. If email is already in use by a different
        // auth method, refuse — user must sign in the original way (or implement
        // account linking later).
        if (email != null && userDao.findByEmail(email).isPresent()) {
            throw new ConflictException(
                    "An account with this email already exists. Sign in with your original method.");
        }

        User u = new User();
        u.setEmail(email != null ? email.toLowerCase() : provider + ":" + subject);
        u.setDisplayName(name != null ? name : "User");
        u.setEmailVerified(email != null);
        u.setOauthProvider(provider);
        u.setOauthSubject(subject);
        u.setTimezone(timezone != null ? timezone : "UTC");

        try {
            User saved = userDao.insert(u);
            issueTokens(saved, response);
            response.setHeader("X-Marginalia-Account", "created");
            return saved;
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Account already exists");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Refresh
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public User refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = readCookie(request, SecurityConstants.REFRESH_TOKEN_COOKIE);
        if (token == null) {
            throw new UnauthorizedException("Missing refresh token");
        }

        UUID userId;
        try {
            String type = jwtService.extractType(token);
            if (!"refresh".equals(type)) {
                // An access token in the refresh cookie slot — refuse rather than upgrade.
                throw new UnauthorizedException("Wrong token type");
            }
            userId = jwtService.extractUserId(token);
        } catch (UnauthorizedException pass) {
            throw pass;
        } catch (Exception e) {
            // Expired, bad signature, malformed — all map to 401.
            throw new UnauthorizedException("Refresh token invalid or expired");
        }

        // User may have been deleted since the refresh was issued.
        User u = userDao.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        userDao.updateLastSeen(u.getId());
        issueTokens(u, response);   // rotates both — old refresh value is overwritten
        return u;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void logout(HttpServletResponse response) {
        clearCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE);
        clearCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE);
    }

    @Override
    public void requestPasswordReset(ForgotPasswordRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        Optional<User> found = userDao.findByEmail(email);

        // Always return the same response so this endpoint cannot be used to
        // enumerate accounts or discover their authentication method.
        if (found.isEmpty() || found.get().getPasswordHash() == null) {
            return;
        }

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        passwordResetTokenDao.replaceForUser(
                found.get().getId(),
                sha256(rawToken),
                OffsetDateTime.now().plusMinutes(30));
        passwordResetEmailService.send(email, rawToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        UUID userId = passwordResetTokenDao.consume(sha256(req.getToken()))
                .orElseThrow(() -> new BadRequestException(
                        "This recovery link is invalid or has expired."));
        userDao.updatePassword(userId, passwordEncoder.encode(req.getPassword()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cookie helpers
    // ─────────────────────────────────────────────────────────────────────
    private void issueTokens(User user, HttpServletResponse response) {
        String access  = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken(user.getId());

        setCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE,  access,
                (int) (jwtService.getAccessTokenExpirationMs() / 1000));
        setCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, refresh,
                (int) (jwtService.getRefreshTokenExpirationMs() / 1000));
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        Cookie c = new Cookie(name, value);
        c.setHttpOnly(true);
        c.setSecure(false); // TODO: true in prod (HTTPS-only)
        c.setPath("/");
        c.setMaxAge(maxAgeSec);
        c.setAttribute("SameSite", "Lax");
        response.addCookie(c);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie c = new Cookie(name, "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
    }

    private static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
