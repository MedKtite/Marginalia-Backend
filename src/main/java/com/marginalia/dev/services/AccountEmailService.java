package com.marginalia.dev.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Account & security emails (Figma "Readsake · Emails", 01–05). Brevo fills the template with the params and
 * sends it from the template's sender (no-reply@readsake.io). The HTML lives in email-template/brevo and the
 * Brevo ID of each template in email/brevo-templates.properties, both maintained by
 * email-template/upload_to_brevo.py.
 *
 * Without BREVO_API_KEY the email is only written to the log: local development, links can be copied from there.
 */
@Service
public class AccountEmailService {

    public enum Template { VERIFY_EMAIL, WELCOME, PASSWORD_RESET, PASSWORD_CHANGED, ACCOUNT_DELETED }

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final RestClient restClient;
    private final String apiKey;
    private final Properties templateIds = new Properties();

    @Value("${app.email-verification-url:marginalia://app/verify-email}")
    private String verificationUrl;

    @Value("${app.password-reset-url:marginalia://app/reset-password}")
    private String passwordResetUrl;

    @Value("${app.open-url:https://readsake.io}")
    private String openUrl;

    public AccountEmailService(@Value("${brevo.api-url:https://api.brevo.com/v3}") String apiUrl,
                               @Value("${brevo.api-key:}") String apiKey) throws IOException {
        // Spring Boot 4 only auto-configures a RestClient.Builder with spring-boot-restclient; this is enough
        this.restClient = RestClient.builder().baseUrl(apiUrl).build();
        this.apiKey = apiKey;
        ClassPathResource ids = new ClassPathResource("email/brevo-templates.properties");
        if (ids.exists()) {
            try (InputStream in = ids.getInputStream()) {
                templateIds.load(in);
            }
        }
        // With a key, fail at startup rather than on the first email of a template that was never uploaded
        List<String> missing = Arrays.stream(Template.values()).map(Enum::name)
                .filter(t -> templateIds.getProperty(t) == null).toList();
        if (sending() && !missing.isEmpty()) {
            throw new IllegalStateException("No Brevo template for " + missing
                    + ": run email-template/upload_to_brevo.py");
        }
    }

    @Async
    public void sendVerification(String email, String displayName, String rawToken) {
        send(Template.VERIFY_EMAIL, email, displayName, Map.of(
                "name", firstName(displayName),
                "link", withToken(verificationUrl, rawToken)));
    }

    /** After the email is confirmed, or on the first sign-in of a new Google / X account. */
    @Async
    public void sendWelcome(String email, String displayName) {
        send(Template.WELCOME, email, displayName, Map.of(
                "name", firstName(displayName),
                "link", openUrl));
    }

    @Async
    public void sendPasswordReset(String email, String rawToken) {
        send(Template.PASSWORD_RESET, email, null, Map.of(
                "link", withToken(passwordResetUrl, rawToken)));
    }

    /** {@code timezone} is the user's IANA zone: the change time is shown in it. */
    @Async
    public void sendPasswordChanged(String email, String timezone) {
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone != null ? timezone : "UTC");
        } catch (Exception e) {
            zone = ZoneId.of("UTC");
        }
        send(Template.PASSWORD_CHANGED, email, null, Map.of(
                "when", ZonedDateTime.now(zone).format(WHEN) + " (" + zone.getId() + ")"));
    }

    @Async
    public void sendAccountDeleted(String email) {
        send(Template.ACCOUNT_DELETED, email, null, Map.of());
    }

    private void send(Template template, String to, String toName, Map<String, String> extra) {
        if (to == null || !to.contains("@")) {
            return; // X accounts without an email are stored as "x:<id>"
        }
        Map<String, String> params = new HashMap<>(extra);
        params.put("email", to);
        if (!sending()) {
            log.info("EMAIL (not sent, no BREVO_API_KEY) to={} template={} params={}", to, template, params);
            return;
        }
        Map<String, String> recipient = toName != null && !toName.isBlank()
                ? Map.of("email", to, "name", toName.strip()) : Map.of("email", to);
        Map<String, Object> body = Map.of(
                "to", List.of(recipient),
                "templateId", Long.parseLong(templateIds.getProperty(template.name())),
                "params", params);
        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email {} sent to {}", template, to);
        } catch (Exception e) {
            log.error("Email {} to {} failed: {}", template, to, e.getMessage());
        }
    }

    private boolean sending() {
        return apiKey != null && !apiKey.isBlank();
    }

    private static String withToken(String url, String rawToken) {
        return url + (url.contains("?") ? "&" : "?") + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private static String firstName(String displayName) {
        if (displayName == null || displayName.isBlank() || "User".equals(displayName)) {
            return "";
        }
        return displayName.strip().split("\\s+")[0];
    }
}
