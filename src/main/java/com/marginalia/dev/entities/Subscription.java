package com.marginalia.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.subscriptions. One row per user (user_id is PK).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {

    private UUID userId;

    private String tier;          // 'free' | 'pro'

    private String status;        // 'active' | 'in_grace_period' | 'expired' | 'billing_retry' | 'cancelled'

    private String store;         // 'app_store' | 'play_store' | 'stripe'

    private String revenuecatId;

    private String productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime currentPeriodStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime currentPeriodEnd;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime cancelAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime trialEnd;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getRevenuecatId() { return revenuecatId; }
    public void setRevenuecatId(String revenuecatId) { this.revenuecatId = revenuecatId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public OffsetDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(OffsetDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

    public OffsetDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(OffsetDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public OffsetDateTime getCancelAt() { return cancelAt; }
    public void setCancelAt(OffsetDateTime cancelAt) { this.cancelAt = cancelAt; }

    public OffsetDateTime getTrialEnd() { return trialEnd; }
    public void setTrialEnd(OffsetDateTime trialEnd) { this.trialEnd = trialEnd; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
