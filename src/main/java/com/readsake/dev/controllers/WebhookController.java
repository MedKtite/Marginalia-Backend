package com.readsake.dev.controllers;

import com.readsake.dev.dto.subscription.RevenueCatWebhookPayload;
import com.readsake.dev.exceptions.UnauthorizedException;
import com.readsake.dev.services.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook receiver — bypassed by JWT auth (see SecurityConstants.AUTH_WHITELIST).
 * The endpoint does its own shared-secret check via the Authorization header.
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @Value("${revenuecat.webhook-secret:}")
    private String revenuecatSecret;

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/revenuecat")
    public ResponseEntity<Void> revenuecat(HttpServletRequest request,
                                           @RequestBody RevenueCatWebhookPayload payload) {
        verifyWebhookAuthorization(request);
        subscriptionService.processWebhook(payload);
        // 200 OK — RevenueCat retries on non-2xx.
        return ResponseEntity.ok().build();
    }

    /**
     * Compares the Authorization header against the configured shared secret.
     * If the secret is blank (dev/test), accepts any caller — makes curl-testing easy.
     */
    private void verifyWebhookAuthorization(HttpServletRequest request) {
        if (revenuecatSecret == null || revenuecatSecret.isBlank()) {
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.equals(revenuecatSecret)) {
            throw new UnauthorizedException("Webhook authorization failed");
        }
    }
}
