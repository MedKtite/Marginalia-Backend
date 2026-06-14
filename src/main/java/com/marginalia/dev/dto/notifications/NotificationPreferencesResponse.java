package com.marginalia.dev.dto.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.marginalia.dev.entities.NotificationPreferences;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferencesResponse {

    private UUID userId;
    private Boolean enabled;

    /** Inlined as a real JSON object (not a string). */
    @JsonRawValue
    private String categories;

    private Integer ritualTimeLocal;
    private Integer passageTimeLocal;
    private Integer quietStartLocal;
    private Integer quietEndLocal;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public static NotificationPreferencesResponse from(NotificationPreferences p) {
        NotificationPreferencesResponse r = new NotificationPreferencesResponse();
        r.userId           = p.getUserId();
        r.enabled          = p.getEnabled();
        r.categories       = p.getCategories();
        r.ritualTimeLocal  = p.getRitualTimeLocal();
        r.passageTimeLocal = p.getPassageTimeLocal();
        r.quietStartLocal  = p.getQuietStartLocal();
        r.quietEndLocal    = p.getQuietEndLocal();
        r.updatedAt        = p.getUpdatedAt();
        return r;
    }

    public UUID getUserId() { return userId; }
    public Boolean getEnabled() { return enabled; }
    public String getCategories() { return categories; }
    public Integer getRitualTimeLocal() { return ritualTimeLocal; }
    public Integer getPassageTimeLocal() { return passageTimeLocal; }
    public Integer getQuietStartLocal() { return quietStartLocal; }
    public Integer getQuietEndLocal() { return quietEndLocal; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
