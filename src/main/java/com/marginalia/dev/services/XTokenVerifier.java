package com.marginalia.dev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marginalia.dev.exceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies an X (Twitter) OAuth 2.0 access token by calling /2/users/me with
 * Bearer auth. The endpoint succeeds only for valid, non-revoked tokens, which
 * is sufficient to prove ownership of the X account.
 * <p>
 * Note: X does not return email via /2/users/me. If you need email, request the
 * elevated "user.email" scope via Twitter's API v1.1 — out of scope here.
 */
@Component
public class XTokenVerifier {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public Result verify(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.x.com/2/users/me?user.fields=name,username"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new UnauthorizedException("Invalid X token");
            }

            JsonNode data = mapper.readTree(resp.body()).path("data");
            String id       = data.path("id").asText(null);
            String name     = data.path("name").asText(null);
            String username = data.path("username").asText(null);

            if (id == null || id.isBlank()) {
                throw new UnauthorizedException("X token missing user id");
            }

            return new Result(id, null, name != null ? name : username);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Failed to verify X token");
        }
    }

    /**
     * @param subject X user id (matches oauth_subject column)
     * @param email   always null (X doesn't expose email via /2/users/me)
     * @param name    display name or @username fallback
     */
    public record Result(String subject, String email, String name) {}
}
