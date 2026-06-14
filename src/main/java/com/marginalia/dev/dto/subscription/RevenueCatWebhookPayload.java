package com.marginalia.dev.dto.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the RevenueCat webhook payload. We only model fields we act on;
 * the rest is ignored thanks to @JsonIgnoreProperties.
 *
 * Full payload reference: https://docs.revenuecat.com/docs/webhooks
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueCatWebhookPayload {

    @JsonProperty("api_version")
    private String apiVersion;

    private Event event;

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {

        private String id;

        /** INITIAL_PURCHASE | RENEWAL | CANCELLATION | EXPIRATION | BILLING_ISSUE |
         *  PRODUCT_CHANGE | UNCANCELLATION | SUBSCRIPTION_PAUSED | TRANSFER | ... */
        private String type;

        @JsonProperty("app_user_id")
        private String appUserId;

        @JsonProperty("original_app_user_id")
        private String originalAppUserId;

        @JsonProperty("product_id")
        private String productId;

        /** APP_STORE | PLAY_STORE | STRIPE | ... */
        private String store;

        /** PRODUCTION | SANDBOX */
        private String environment;

        @JsonProperty("purchased_at_ms")
        private Long purchasedAtMs;

        @JsonProperty("expiration_at_ms")
        private Long expirationAtMs;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getAppUserId() { return appUserId; }
        public void setAppUserId(String appUserId) { this.appUserId = appUserId; }

        public String getOriginalAppUserId() { return originalAppUserId; }
        public void setOriginalAppUserId(String originalAppUserId) { this.originalAppUserId = originalAppUserId; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getStore() { return store; }
        public void setStore(String store) { this.store = store; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public Long getPurchasedAtMs() { return purchasedAtMs; }
        public void setPurchasedAtMs(Long purchasedAtMs) { this.purchasedAtMs = purchasedAtMs; }

        public Long getExpirationAtMs() { return expirationAtMs; }
        public void setExpirationAtMs(Long expirationAtMs) { this.expirationAtMs = expirationAtMs; }
    }
}
