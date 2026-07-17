package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.auth.ForgotPasswordRequest;
import com.marginalia.dev.dto.auth.LoginRequest;
import com.marginalia.dev.dto.auth.OAuthLoginRequest;
import com.marginalia.dev.dto.auth.RegisterRequest;
import com.marginalia.dev.dto.auth.ResetPasswordRequest;
import com.marginalia.dev.dto.auth.UserResponse;
import com.marginalia.dev.entities.User;
import com.marginalia.dev.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 HttpServletResponse response) {
        User u = authService.register(req, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(u));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest req,
                                              HttpServletResponse response) {
        User u = authService.login(req, response);
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<UserResponse> loginGoogle(@Valid @RequestBody OAuthLoginRequest req,
                                                    HttpServletResponse response) {
        User u = authService.loginWithGoogle(req, response);
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PostMapping("/oauth/x")
    public ResponseEntity<UserResponse> loginX(@Valid @RequestBody OAuthLoginRequest req,
                                               HttpServletResponse response) {
        User u = authService.loginWithX(req, response);
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        User u = authService.refresh(request, response);
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.requestPasswordReset(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
