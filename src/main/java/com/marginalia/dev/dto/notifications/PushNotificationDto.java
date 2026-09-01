package com.marginalia.dev.dto.notifications;

import java.io.Serializable;
import java.util.Map;

public class PushNotificationDto implements Serializable {

    private String token;
    private String title;
    private String body;
    private String category;
    private Map<String, String> data;

    public PushNotificationDto() {
    }

    public PushNotificationDto(String token, String title, String body, String category, Map<String, String> data) {
        this.token = token;
        this.title = title;
        this.body = body;
        this.category = category;
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
