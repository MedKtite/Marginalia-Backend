package com.marginalia.dev.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

/**
 * The mobile client obtains a token from the OAuth provider's SDK and POSTs it here.
 * For Google: this is the Google ID token (JWT).
 * For X: this is the OAuth 2.0 access token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthLoginRequest {

    @NotBlank
    private String token;

    private String timezone; // optional

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
