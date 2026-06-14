package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.preferences.UserPreferencesResponse;
import com.marginalia.dev.dto.preferences.UserPreferencesUpdateRequest;
import com.marginalia.dev.entities.UserPreferences;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.UserPreferencesService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me/preferences")
public class UserPreferencesController {

    @Autowired
    private UserPreferencesService prefsService;

    @GetMapping
    public UserPreferencesResponse get(@AuthenticationPrincipal CustomUserDetails me) {
        UserPreferences p = prefsService.get(me.getUserId());
        return UserPreferencesResponse.from(p);
    }

    @PatchMapping
    public UserPreferencesResponse update(@AuthenticationPrincipal CustomUserDetails me,
                                          @Valid @RequestBody UserPreferencesUpdateRequest req) {
        UserPreferences p = prefsService.update(me.getUserId(), req);
        return UserPreferencesResponse.from(p);
    }
}
