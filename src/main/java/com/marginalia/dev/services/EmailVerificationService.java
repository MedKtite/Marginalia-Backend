package com.marginalia.dev.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:ktite.m3@gmail.com}")
    private String from;

    @Value("${app.email-verification-url:marginalia://app/verify-email}")
    private String verificationUrl;

    public EmailVerificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Async
    public void sendVerificationEmail(String email, String rawToken, String displayName) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("JavaMailSender is not configured. Verification email was not sent to {}", email);
            return;
        }

        try {
            String link = verificationUrl + "?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("Verify your email — Marginalia");
            message.setText("""
                    Hello %s,

                    Welcome to Marginalia. Please confirm your email address using the link below:
                    %s

                    This link will expire in 24 hours.

                    If you did not sign up for Marginalia, you can safely ignore this email.

                    — The Marginalia Team
                    """.formatted(displayName != null && !displayName.isBlank() ? displayName : "Reader", link));

            sender.send(message);
            log.info("Verification email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage(), e);
        }
    }
}
