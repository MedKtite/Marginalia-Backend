package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.notifications.NotificationPreferencesResponse;
import com.marginalia.dev.dto.notifications.NotificationPreferencesUpdateRequest;
import com.marginalia.dev.entities.NotificationPreferences;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.NotificationPreferencesService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me/notifications")
public class NotificationPreferencesController {

    @Autowired
    private NotificationPreferencesService prefsService;

    @GetMapping
    public NotificationPreferencesResponse get(@AuthenticationPrincipal CustomUserDetails me) {
        NotificationPreferences p = prefsService.get(me.getUserId());
        return NotificationPreferencesResponse.from(p);
    }

    @PatchMapping
    public NotificationPreferencesResponse update(@AuthenticationPrincipal CustomUserDetails me,
                                                  @Valid @RequestBody NotificationPreferencesUpdateRequest req) {
        NotificationPreferences p = prefsService.update(me.getUserId(), req);
        return NotificationPreferencesResponse.from(p);
    }
}
