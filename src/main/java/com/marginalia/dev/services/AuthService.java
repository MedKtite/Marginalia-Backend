package com.marginalia.dev.services;

import com.marginalia.dev.dto.auth.ForgotPasswordRequest;
import com.marginalia.dev.dto.auth.LoginRequest;
import com.marginalia.dev.dto.auth.OAuthLoginRequest;
import com.marginalia.dev.dto.auth.RegisterRequest;
import com.marginalia.dev.dto.auth.ResetPasswordRequest;
import com.marginalia.dev.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface AuthService {

    User register(RegisterRequest req, HttpServletResponse response);

    User login(LoginRequest req, HttpServletResponse response);

    User loginWithGoogle(OAuthLoginRequest req, HttpServletResponse response);

    User loginWithX(OAuthLoginRequest req, HttpServletResponse response);

    /**
     * Validates the {@code marg_refresh} cookie, rotates both tokens, returns
     * the user. Throws {@code UnauthorizedException} for any failure
     * (missing cookie, expired, signature mismatch, wrong type, user gone).
     */
    User refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletResponse response);

    void requestPasswordReset(ForgotPasswordRequest req);

    void resetPassword(ResetPasswordRequest req);

    void verifyEmail(String token);

    void resendVerificationEmail(UUID userId);
}
