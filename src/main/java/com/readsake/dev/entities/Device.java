package com.readsake.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.devices. Tracks FCM push tokens, sync conflict
 * resolution data, and the 5-device Pro cap.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

    private UUID id;

    private UUID userId;

    private String platform;        // 'ios' | 'android' | 'web'

    private String deviceName;

    private String pushToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime fcmTokenUpdatedAt;

    private String pushPermission;  // 'granted' | 'denied' | 'not_determined' | 'provisional'

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime firstSeenAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastSeenAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }

    public OffsetDateTime getFcmTokenUpdatedAt() { return fcmTokenUpdatedAt; }
    public void setFcmTokenUpdatedAt(OffsetDateTime fcmTokenUpdatedAt) { this.fcmTokenUpdatedAt = fcmTokenUpdatedAt; }

    public String getPushPermission() { return pushPermission; }
    public void setPushPermission(String pushPermission) { this.pushPermission = pushPermission; }

    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(OffsetDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
