package com.readsake.dev.dto.subscription;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.readsake.dev.entities.Subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Safe public view of a subscription. Free users see a synthetic
 * "tier=free, status=null" response — clients always get a well-formed body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {

    private UUID userId;
    private String tier;          // "free" | "pro"
    private String status;        // "active" | "in_grace_period" | "expired" | "billing_retry" | "cancelled" | null
    private String store;
    private String productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime currentPeriodStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime currentPeriodEnd;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime cancelAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime trialEnd;

    public static SubscriptionResponse from(Subscription s) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.userId             = s.getUserId();
        r.tier               = s.getTier();
        r.status             = s.getStatus();
        r.store              = s.getStore();
        r.productId          = s.getProductId();
        r.currentPeriodStart = s.getCurrentPeriodStart();
        r.currentPeriodEnd   = s.getCurrentPeriodEnd();
        r.cancelAt           = s.getCancelAt();
        r.trialEnd           = s.getTrialEnd();
        return r;
    }

    public static SubscriptionResponse freeFor(UUID userId) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.userId = userId;
        r.tier   = "free";
        return r;
    }

    public UUID getUserId() { return userId; }
    public String getTier() { return tier; }
    public String getStatus() { return status; }
    public String getStore() { return store; }
    public String getProductId() { return productId; }
    public OffsetDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public OffsetDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public OffsetDateTime getCancelAt() { return cancelAt; }
    public OffsetDateTime getTrialEnd() { return trialEnd; }
}
