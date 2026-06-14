package com.marginalia.dev.dto.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * PATCH semantics — null fields leave the stored value untouched.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationPreferencesUpdateRequest {

    private Boolean enabled;

    /** JSON object — stored as JSONB. e.g. {"ritual":true,"passage":false,...} */
    private String categories;

    @Min(0) @Max(23)
    private Integer ritualTimeLocal;

    @Min(0) @Max(23)
    private Integer passageTimeLocal;

    @Min(0) @Max(23)
    private Integer quietStartLocal;

    @Min(0) @Max(23)
    private Integer quietEndLocal;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getCategories() { return categories; }

    @JsonSetter("categories")
    public void setCategories(JsonNode node) {
        this.categories = node == null || node.isNull() ? null : node.toString();
    }

    public Integer getRitualTimeLocal() { return ritualTimeLocal; }
    public void setRitualTimeLocal(Integer ritualTimeLocal) { this.ritualTimeLocal = ritualTimeLocal; }

    public Integer getPassageTimeLocal() { return passageTimeLocal; }
    public void setPassageTimeLocal(Integer passageTimeLocal) { this.passageTimeLocal = passageTimeLocal; }

    public Integer getQuietStartLocal() { return quietStartLocal; }
    public void setQuietStartLocal(Integer quietStartLocal) { this.quietStartLocal = quietStartLocal; }

    public Integer getQuietEndLocal() { return quietEndLocal; }
    public void setQuietEndLocal(Integer quietEndLocal) { this.quietEndLocal = quietEndLocal; }
}
