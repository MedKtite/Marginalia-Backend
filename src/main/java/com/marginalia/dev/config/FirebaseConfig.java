package com.marginalia.dev.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase wiring is conditional on {@code firebase.service-account-path}
 * being a non-blank string. When unset (typical in dev), this entire
 * configuration is skipped — no FirebaseApp / FirebaseMessaging beans are
 * created, and {@link com.marginalia.dev.services.FcmNotificationSender}
 * (which autowires FirebaseMessaging with {@code required = false}) falls
 * back to DRY-RUN logging.
 *
 * In prod, set the env var to the absolute path of your service-account
 * JSON. A bad path causes a loud, fatal startup error — better than
 * silently swallowing the misconfig and discovering it three days later
 * when push delivery numbers are zero.
 */
@Configuration
@ConditionalOnExpression("'${firebase.service-account-path:}' != ''")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        try (FileInputStream stream = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions opts = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            // Reuse the default app if already initialized (e.g. during DevTools restart).
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }
            FirebaseApp app = FirebaseApp.initializeApp(opts);
            log.info("Firebase Admin SDK initialized from {}", serviceAccountPath);
            return app;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
