package com.marginalia.dev.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sent from the Flutter app on first run + whenever the FCM token rotates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceRegisterRequest {

    @NotBlank
    @Pattern(regexp = "ios|android|web")
    private String platform;

    @Size(max = 120)
    private String deviceName;

    /** FCM token — may be null when permission was denied. */
    private String pushToken;

    @Pattern(regexp = "granted|denied|not_determined|provisional")
    private String pushPermission;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }

    public String getPushPermission() { return pushPermission; }
    public void setPushPermission(String pushPermission) { this.pushPermission = pushPermission; }
}
