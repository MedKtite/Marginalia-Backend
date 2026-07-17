package com.marginalia.dev.services;

import com.marginalia.dev.dto.subscription.RevenueCatWebhookPayload;
import com.marginalia.dev.dto.subscription.SubscriptionResponse;
import com.marginalia.dev.entities.Subscription;
import com.marginalia.dev.repo.dao.SubscriptionDao;
import com.marginalia.dev.repo.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private UserDao userDao;

    @Override
    public SubscriptionResponse getForUser(UUID userId) {
        return subscriptionDao.findByUserId(userId)
                .map(SubscriptionResponse::from)
                .orElseGet(() -> SubscriptionResponse.freeFor(userId));
    }

    @Override
    @Transactional
    public void processWebhook(RevenueCatWebhookPayload payload) {
        if (payload == null || payload.getEvent() == null) {
            log.warn("RevenueCat webhook with no event payload — ignoring");
            return;
        }

        RevenueCatWebhookPayload.Event ev = payload.getEvent();
        UUID userId;
        try {
            userId = UUID.fromString(ev.getAppUserId());
        } catch (Exception e) {
            log.warn("RevenueCat webhook with non-UUID app_user_id [{}] — ignoring", ev.getAppUserId());
            return;
        }

        // If the user doesn't exist on our side (could happen with a stale TRANSFER), drop the event.
        if (userDao.findById(userId).isEmpty()) {
            log.warn("RevenueCat webhook for unknown user [{}] — ignoring", userId);
            return;
        }

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setRevenuecatId(ev.getAppUserId());
        sub.setProductId(ev.getProductId());
        sub.setStore(mapStore(ev.getStore()));
        sub.setCurrentPeriodStart(toOffsetDateTime(ev.getPurchasedAtMs()));
        sub.setCurrentPeriodEnd(toOffsetDateTime(ev.getExpirationAtMs()));
        if ("TRIAL".equalsIgnoreCase(ev.getPeriodType())) {
            sub.setTrialEnd(toOffsetDateTime(ev.getExpirationAtMs()));
        }

        String t = ev.getType() == null ? "" : ev.getType().toUpperCase();
        switch (t) {
            case "INITIAL_PURCHASE":
            case "RENEWAL":
            case "UNCANCELLATION":
            case "PRODUCT_CHANGE":
                sub.setTier("pro");
                sub.setStatus("active");
                // cancel_at intentionally cleared by NULL in upsert
                break;

            case "CANCELLATION":
                // User cancelled but still has paid period — keep pro+active, surface cancel_at.
                sub.setTier("pro");
                sub.setStatus("active");
                sub.setCancelAt(toOffsetDateTime(ev.getExpirationAtMs()));
                break;

            case "EXPIRATION":
                sub.setTier("free");
                sub.setStatus("expired");
                break;

            case "BILLING_ISSUE":
                sub.setTier("pro");
                sub.setStatus("billing_retry");
                break;

            case "SUBSCRIPTION_PAUSED":
                sub.setTier("pro");
                sub.setStatus("in_grace_period");
                break;

            default:
                log.info("RevenueCat event type [{}] — no action taken", t);
                return;
        }

        subscriptionDao.upsert(sub);
        log.info("RevenueCat: user={} type={} → tier={} status={}",
                userId, t, sub.getTier(), sub.getStatus());
    }

    private static String mapStore(String revenuecatStore) {
        if (revenuecatStore == null) return null;
        return switch (revenuecatStore.toUpperCase()) {
            case "APP_STORE"      -> "app_store";
            case "PLAY_STORE"     -> "play_store";
            case "STRIPE"         -> "stripe";
            default               -> null;
        };
    }

    private static OffsetDateTime toOffsetDateTime(Long millis) {
        return millis == null ? null
                : OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }
}
