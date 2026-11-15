package com.readsake.dev.services;

import com.readsake.dev.dto.subscription.RevenueCatWebhookPayload;
import com.readsake.dev.dto.subscription.SubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse getForUser(UUID userId);

    /** Idempotent: a re-delivered webhook for the same event lands on the same end-state. */
    void processWebhook(RevenueCatWebhookPayload payload);
}
