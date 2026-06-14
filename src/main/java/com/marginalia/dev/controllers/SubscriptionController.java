package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.subscription.SubscriptionResponse;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Always returns a body — free users get tier="free" with the other fields null.
     * The mobile app reads `tier` and flips Pro-only features on/off.
     */
    @GetMapping
    public SubscriptionResponse get(@AuthenticationPrincipal CustomUserDetails me) {
        return subscriptionService.getForUser(me.getUserId());
    }
}
