package com.readsake.dev.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors row in public.notification_preferences. The categories column is
 * JSONB — kept as raw JSON string here; service layer parses to a map.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationPreferences {

    private UUID userId;

    private Boolean enabled;

    private String categories;       // JSONB as raw string

    private Integer ritualTimeLocal;

    private Integer passageTimeLocal;

    private Integer quietStartLocal;

    private Integer quietEndLocal;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public Integer getRitualTimeLocal() { return ritualTimeLocal; }
    public void setRitualTimeLocal(Integer ritualTimeLocal) { this.ritualTimeLocal = ritualTimeLocal; }

    public Integer getPassageTimeLocal() { return passageTimeLocal; }
    public void setPassageTimeLocal(Integer passageTimeLocal) { this.passageTimeLocal = passageTimeLocal; }

    public Integer getQuietStartLocal() { return quietStartLocal; }
    public void setQuietStartLocal(Integer quietStartLocal) { this.quietStartLocal = quietStartLocal; }

    public Integer getQuietEndLocal() { return quietEndLocal; }
    public void setQuietEndLocal(Integer quietEndLocal) { this.quietEndLocal = quietEndLocal; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
