package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.insights.InsightsResponse;
import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.InsightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/insights")
public class InsightsController {

    @Autowired
    private InsightsService insightsService;

    @GetMapping
    public InsightsResponse get(@AuthenticationPrincipal CustomUserDetails me) {
        return insightsService.forUser(me.getUserId());
    }
}
