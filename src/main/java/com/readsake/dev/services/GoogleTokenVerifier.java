package com.readsake.dev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readsake.dev.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Verifies a Google ID token by calling Google's tokeninfo endpoint.
 * If {@code google.client-id} is set, the token's aud claim must match — required
 * in production. Returns the user's Google subject (stable id), email (if verified),
 * and name.
 */
@Component
public class GoogleTokenVerifier {

    @Value("${google.client-id:}")
    private String allowedClientId;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public Result verify(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" +
                    URLEncoder.encode(idToken, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new UnauthorizedException("Invalid Google token");
            }

            JsonNode body = mapper.readTree(resp.body());

            String aud           = body.path("aud").asText("");
            String sub           = body.path("sub").asText(null);
            String email         = body.path("email").asText(null);
            String name          = body.path("name").asText(null);
            boolean emailVerified = body.path("email_verified").asBoolean(false);

            if (allowedClientId != null && !allowedClientId.isBlank()
                    && !allowedClientId.equals(aud)) {
                throw new UnauthorizedException("Google token audience mismatch");
            }
            if (sub == null || sub.isBlank()) {
                throw new UnauthorizedException("Google token missing subject");
            }

            return new Result(sub, emailVerified ? email : null, name);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Failed to verify Google token");
        }
    }

    /**
     * @param subject Google's stable user id (matches oauth_subject column)
     * @param email   verified email or null
     * @param name    display name from Google profile or null
     */
    public record Result(String subject, String email, String name) {}
}
