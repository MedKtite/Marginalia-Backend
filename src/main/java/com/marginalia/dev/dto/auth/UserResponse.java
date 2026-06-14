package com.marginalia.dev.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.marginalia.dev.entities.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Safe public view of a User — never exposes passwordHash or internal columns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String email;
    private Boolean emailVerified;
    private String displayName;
    private String shortName;
    private String avatarInitial;
    private String timezone;
    private String authProvider; // "password" | "google" | "x" | ...

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id            = u.getId();
        r.email         = u.getEmail();
        r.emailVerified = u.getEmailVerified();
        r.displayName   = u.getDisplayName();
        r.shortName     = u.getShortName();
        r.avatarInitial = u.getAvatarInitial();
        r.timezone      = u.getTimezone();
        r.authProvider  = u.getOauthProvider() != null ? u.getOauthProvider() : "password";
        r.createdAt     = u.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getAvatarInitial() { return avatarInitial; }
    public void setAvatarInitial(String avatarInitial) { this.avatarInitial = avatarInitial; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
