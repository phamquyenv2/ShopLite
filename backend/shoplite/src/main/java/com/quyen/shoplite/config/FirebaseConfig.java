package com.quyen.shoplite.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${shoplite.firebase.credentials-file:}")
    private String credentialsFile;

    @Value("${shoplite.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${shoplite.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.warn("[Firebase] Firebase is DISABLED in config. Push notifications will be mocked.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(resolveCredentials())
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[Firebase] FirebaseApp initialized successfully.");
            } catch (IOException e) {
                log.error("[Firebase] Failed to initialize Firebase. Push notifications will not work. Error: {}", e.getMessage());
            }
        } else {
            log.info("[Firebase] FirebaseApp already initialized. Skipping.");
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (credentialsFile != null && !credentialsFile.isBlank()) {
            log.info("[Firebase] Loading credentials from file: {}", credentialsFile);
            try (InputStream serviceAccount = new FileInputStream(credentialsFile)) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        if (credentialsJson != null && !credentialsJson.isBlank()) {
            log.info("[Firebase] Loading credentials from inline JSON.");
            try (InputStream serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        InputStream classpathStream = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");
        if (classpathStream != null) {
            log.info("[Firebase] Loading credentials from classpath: firebase-service-account.json");
            try (InputStream serviceAccount = classpathStream) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        log.info("[Firebase] Loading credentials from Application Default Credentials.");
        return GoogleCredentials.getApplicationDefault();
    }
}
