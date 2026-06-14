package com.marginalia.dev.services;

import com.marginalia.dev.dto.auth.LoginRequest;
import com.marginalia.dev.dto.auth.OAuthLoginRequest;
import com.marginalia.dev.dto.auth.RegisterRequest;
import com.marginalia.dev.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
}
