package com.readsake.dev.controllers;

import com.readsake.dev.dto.auth.ForgotPasswordRequest;
import com.readsake.dev.dto.auth.LoginRequest;
import com.readsake.dev.dto.auth.OAuthLoginRequest;
import com.readsake.dev.dto.auth.RegisterRequest;
import com.readsake.dev.dto.auth.ResetPasswordRequest;
import com.readsake.dev.dto.auth.UserResponse;
import com.readsake.dev.dto.auth.VerifyEmailRequest;
import com.readsake.dev.entities.User;
import com.readsake.dev.security.CustomUserDetails;
import com.readsake.dev.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.getToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmailGet(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Email confirmed — Readsake</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, serif; background: #FAF8F5; color: #1C1C1E; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                        .card { background: #FFFFFF; padding: 40px; border-radius: 16px; border: 1px solid rgba(28,28,30,0.08); text-align: center; max-width: 400px; box-shadow: 0 4px 12px rgba(28,28,30,0.04); }
                        h1 { font-family: Georgia, serif; font-size: 24px; font-weight: normal; margin-bottom: 12px; }
                        p { color: rgba(28,28,30,0.6); font-size: 15px; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Email confirmed</h1>
                        <p>Your email address has been successfully verified. You can return to the Readsake app.</p>
                    </div>
                </body>
                </html>
                """);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @AuthenticationPrincipal CustomUserDetails me) {
        if (me != null) {
            authService.resendVerificationEmail(me.getUserId());
        }
        return ResponseEntity.noContent().build();
    }
}
