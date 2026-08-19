package com.marginalia.dev.controllers;

import com.marginalia.dev.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestBody Map<String, Object> payload) {

        String category = (String) payload.getOrDefault("category", "general");
        String message = (String) payload.getOrDefault("message", "");
        String email = (String) payload.getOrDefault("email", me != null ? me.getUsername() : "anonymous");
        String appVersion = (String) payload.getOrDefault("app_version", "1.0.0");
        String deviceModel = (String) payload.getOrDefault("device_model", "Unknown");

        String ticketId = "TICK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("New Support/Feedback Ticket [{}] from [{}]: Category='{}', Device='{}', AppVersion='{}', Message='{}'",
                ticketId, email, category, deviceModel, appVersion, message);

        return ResponseEntity.ok(Map.of(
                "status", "received",
                "ticket_id", ticketId,
                "created_at", OffsetDateTime.now().toString(),
                "message", "Thank you for your feedback. Our team will look into it."
        ));
    }
}
