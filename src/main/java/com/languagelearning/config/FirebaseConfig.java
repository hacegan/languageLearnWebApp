package com.languagelearning.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:serviceAccountKey.json}")
    private String firebaseConfigPath;

    private boolean isInitialized = false;

    @PostConstruct
    public void initialize() {
        if (isInitialized || !FirebaseApp.getApps().isEmpty()) {
            System.out.println("Firebase already initialized");
            return;
        }

        try {
            InputStream serviceAccount = null;

            try {
                // Önce root dizinden okumayı dene
                File file = new File(firebaseConfigPath);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                    System.out.println("Loading Firebase config from file system: " + file.getAbsolutePath());
                } else {
                    // Classpath'ten okumayı dene
                    Resource resource = new ClassPathResource(firebaseConfigPath);
                    if (resource.exists()) {
                        serviceAccount = resource.getInputStream();
                        System.out.println("Loading Firebase config from classpath: " + firebaseConfigPath);
                    }
                }

                if (serviceAccount == null) {
                    // Son çare olarak classloader'dan dene
                    serviceAccount = getClass().getClassLoader().getResourceAsStream(firebaseConfigPath);
                    if (serviceAccount != null) {
                        System.out.println("Loading Firebase config from classloader: " + firebaseConfigPath);
                    }
                }

                if (serviceAccount != null) {
                    // GoogleCredentials oluştur
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

                    FirebaseApp.initializeApp(options);
                    isInitialized = true;
                    System.out.println("Firebase initialized successfully!");

                } else {
                    throw new RuntimeException("Firebase service account file not found at: " + firebaseConfigPath);
                }

            } finally {
                // Stream'i kapat
                if (serviceAccount != null) {
                    try {
                        serviceAccount.close();
                    } catch (IOException e) {
                        System.err.println("Error closing service account stream: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("ERROR initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    public Firestore firestore() {
        // Firebase'in initialize olduğundan emin ol
        if (!isInitialized && FirebaseApp.getApps().isEmpty()) {
            initialize();
        }

        if (FirebaseApp.getApps().isEmpty()) {
            throw new RuntimeException("Firebase is not initialized. Please check your configuration.");
        }

        Firestore firestore = FirestoreClient.getFirestore();
        System.out.println("Firestore bean created successfully");
        return firestore;
    }
}