package com.marginalia.dev.services;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordResetEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@marginalia.app}")
    private String from;

    @Value("${app.password-reset-url:marginalia://app/reset-password}")
    private String passwordResetUrl;

    public PasswordResetEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void send(String email, String rawToken) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP is not configured");
        }

        String link = passwordResetUrl + "?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Reset your Marginalia password");
        message.setText("""
                We received a request to reset your Marginalia password.

                Open this link to choose a new password:
                %s

                This link expires in 30 minutes. If you did not request it, you can ignore this email.
                """.formatted(link));
        sender.send(message);
    }
}
