package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.auth.UserResponse;
import com.marginalia.dev.dto.profile.ChangePasswordRequest;
import com.marginalia.dev.dto.profile.ProfileUpdateRequest;
import com.marginalia.dev.entities.User;
import com.marginalia.dev.repo.dao.UserDao;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.AccountEmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/me")
public class ProfileController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountEmailService accountEmailService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal CustomUserDetails me) {
        User u = userDao.findById(me.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody ProfileUpdateRequest req) {

        User updated = userDao.updateProfile(me.getUserId(), req.getDisplayName(), req.getShortName(), req.getTimezone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody ChangePasswordRequest req) {

        User u = userDao.findById(me.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (u.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is configured with social login (OAuth). Password cannot be changed.");
        }

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        String newHash = passwordEncoder.encode(req.getNewPassword());
        userDao.updatePassword(me.getUserId(), newHash);
        accountEmailService.sendPasswordChanged(u.getEmail(), u.getTimezone());

        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    public ResponseEntity<UserResponse> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            byte[] bytes = file.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String dataUri = "data:" + contentType + ";base64," + base64;

            User updated = userDao.updateAvatar(me.getUserId(), dataUri)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            return ResponseEntity.ok(UserResponse.from(updated));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar: " + e.getMessage());
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<UserResponse> deleteAvatar(@AuthenticationPrincipal CustomUserDetails me) {
        User updated = userDao.updateAvatar(me.getUserId(), null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails me) {
        User u = userDao.findById(me.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean deleted = userDao.delete(me.getUserId());
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        accountEmailService.sendAccountDeleted(u.getEmail());
        return ResponseEntity.noContent().build();
    }
}
