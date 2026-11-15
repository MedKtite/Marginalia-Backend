package com.readsake.dev.dto.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readsake.dev.entities.NotificationItem;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;
    private String category;
    private String title;
    private String body;
    private JsonNode data;

    @JsonProperty("is_unread")
    private boolean isUnread;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @JsonProperty("read_at")
    private OffsetDateTime readAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static NotificationResponse from(NotificationItem item) {
        if (item == null) return null;
        NotificationResponse res = new NotificationResponse();
        res.setId(item.getId());
        res.setCategory(item.getCategory());
        res.setTitle(item.getTitle());
        res.setBody(item.getBody());
        res.setUnread(item.getReadAt() == null);
        res.setReadAt(item.getReadAt());
        res.setCreatedAt(item.getCreatedAt());

        if (item.getData() != null && !item.getData().isBlank()) {
            try {
                res.setData(MAPPER.readTree(item.getData()));
            } catch (Exception e) {
                // If json parsing fails, leave null
            }
        }
        return res;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }

    public boolean isUnread() { return isUnread; }
    public void setUnread(boolean unread) { isUnread = unread; }

    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
