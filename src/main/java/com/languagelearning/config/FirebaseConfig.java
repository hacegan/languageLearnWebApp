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

    @Value("${firebase.config.path:/etc/secrets/serviceAccountKey.json}")
    private String firebaseConfigPath;

    private boolean isInitialized = false;

    @PostConstruct
    public void initialize() {
        if (isInitialized || !FirebaseApp.getApps().isEmpty()) {
            System.out.println("Firebase zaten başlatıldı");
            return;
        }

        try {
            InputStream serviceAccount = null;

            try {
                // Önce belirtilen yoldan (ör. /etc/secrets/serviceAccountKey.json) okumayı dene
                File file = new File(firebaseConfigPath);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                    System.out.println("Firebase yapılandırma dosyası dosya sisteminden yüklendi: " + file.getAbsolutePath());
                } else {
                    // Classpath'ten okumayı dene (geliştirme ortamı için)
                    Resource resource = new ClassPathResource("serviceAccountKey.json");
                    if (resource.exists()) {
                        serviceAccount = resource.getInputStream();
                        System.out.println("Firebase yapılandırma dosyası classpath'ten yüklendi: " + firebaseConfigPath);
                    }
                }

                if (serviceAccount == null) {
                    // Son çare olarak classloader'dan dene
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
                    if (serviceAccount != null) {
                        System.out.println("Firebase yapılandırma dosyası classloader'dan yüklendi: " + firebaseConfigPath);
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
                    System.out.println("Firebase başarıyla başlatıldı!");

                } else {
                    throw new RuntimeException("Firebase yapılandırma dosyası bulunamadı: " + firebaseConfigPath);
                }

            } finally {
                // Stream'i kapat
                if (serviceAccount != null) {
                    try {
                        serviceAccount.close();
                    } catch (IOException e) {
                        System.err.println("Yapılandırma dosyası stream'i kapatılırken hata oluştu: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Firebase başlatılırken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Firebase başlatılamadı", e);
        }
    }

    @Bean
    public Firestore firestore() {
        // Firebase'in başlatıldığından emin ol
        if (!isInitialized && FirebaseApp.getApps().isEmpty()) {
            initialize();
        }

        if (FirebaseApp.getApps().isEmpty()) {
            throw new RuntimeException("Firebase başlatılmadı. Lütfen yapılandırmanızı kontrol edin.");
        }

        Firestore firestore = FirestoreClient.getFirestore();
        System.out.println("Firestore bean başarıyla oluşturuldu");
        return firestore;
    }
}