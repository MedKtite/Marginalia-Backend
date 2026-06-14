package com.marginalia.dev.services;

import com.marginalia.dev.dto.subscription.RevenueCatWebhookPayload;
import com.marginalia.dev.dto.subscription.SubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse getForUser(UUID userId);

    /** Idempotent: a re-delivered webhook for the same event lands on the same end-state. */
    void processWebhook(RevenueCatWebhookPayload payload);
}
