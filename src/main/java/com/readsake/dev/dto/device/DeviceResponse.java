package com.readsake.dev.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.readsake.dev.entities.Device;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * pushToken is intentionally NOT exposed — the token is sensitive and the
 * client already has it locally.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse {

    private UUID id;
    private String platform;
    private String deviceName;
    private String pushPermission;
    private Boolean pushTokenPresent;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime firstSeenAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastSeenAt;

    public static DeviceResponse from(Device d) {
        DeviceResponse r = new DeviceResponse();
        r.id               = d.getId();
        r.platform         = d.getPlatform();
        r.deviceName       = d.getDeviceName();
        r.pushPermission   = d.getPushPermission();
        r.pushTokenPresent = d.getPushToken() != null && !d.getPushToken().isBlank();
        r.firstSeenAt      = d.getFirstSeenAt();
        r.lastSeenAt       = d.getLastSeenAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getPlatform() { return platform; }
    public String getDeviceName() { return deviceName; }
    public String getPushPermission() { return pushPermission; }
    public Boolean getPushTokenPresent() { return pushTokenPresent; }
    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
}
