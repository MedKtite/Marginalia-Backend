package com.readsake.dev.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @JsonProperty("display_name")
    @Size(min = 1, max = 100)
    private String displayName;

    @JsonProperty("short_name")
    @Size(max = 20)
    private String shortName;

    private String timezone;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
