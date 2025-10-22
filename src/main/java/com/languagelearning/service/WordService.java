package com.languagelearning.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.languagelearning.model.Word;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class WordService {

    private final Firestore firestore;

    @Autowired
    public WordService(Firestore firestore) {
        this.firestore = firestore;
        System.out.println("WordService initialized with Firestore: " + (firestore != null));
    }

    @PostConstruct
    public void init() {
        if (firestore != null) {
            System.out.println("WordService: Firestore is ready");
        } else {
            System.err.println("WordService: Firestore is NULL!");
        }
    }

    // Test connection method
    public void testConnection() throws Exception {
        if (firestore == null) {
            throw new RuntimeException("Firestore is not initialized");
        }
        try {
            // Try a simple query to test connection
            ApiFuture<QuerySnapshot> future = firestore.collection("test").limit(1).get();
            future.get();
            System.out.println("Firestore connection test successful");
        } catch (Exception e) {
            System.err.println("Firestore connection test failed: " + e.getMessage());
            throw e;
        }
    }

    // Check if Firestore is available
    private void checkFirestore() {
        if (firestore == null) {
            throw new RuntimeException("Firestore connection is not available. Please check your Firebase configuration.");
        }
    }

    // Mevcut kelimeleri güncelle (eksik alanları ekle)
    public void migrateExistingWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            System.out.println("No documents found for migration in " + collectionName);
            return;
        }

        WriteBatch batch = firestore.batch();
        int batchCount = 0;

        for (QueryDocumentSnapshot doc : documents) {
            Map<String, Object> data = doc.getData();
            Map<String, Object> updates = new HashMap<>();

            // Eksik alanları kontrol et ve ekle
            if (!data.containsKey("difficulty")) {
                updates.put("difficulty", "medium");
            }
            if (!data.containsKey("category")) {
                updates.put("category", guessCategory(data.get("word").toString()));
            }
            if (!data.containsKey("incorrectCount")) {
                updates.put("incorrectCount", 0);
            }
            if (!data.containsKey("studyCount")) {
                updates.put("studyCount", 0);
            }
            if (!data.containsKey("isFavorite")) {
                updates.put("isFavorite", false);
            }
            if (!data.containsKey("tags")) {
                updates.put("tags", Arrays.asList("general"));
            }
            if (!data.containsKey("example")) {
                updates.put("example", "");
            }
            if (!data.containsKey("pronunciation")) {
                updates.put("pronunciation", "");
            }

            if (!updates.isEmpty()) {
                batch.update(doc.getReference(), updates);
                batchCount++;

                // Firebase batch limit is 500
                if (batchCount >= 500) {
                    batch.commit().get();
                    batch = firestore.batch();
                    batchCount = 0;
                }
            }
        }

        if (batchCount > 0) {
            batch.commit().get();
        }

        System.out.println("Migration completed for " + collectionName + ": " + documents.size() + " documents processed");
    }

    private String guessCategory(String word) {
        // Simple category guessing based on word endings
        word = word.toLowerCase();
        if (word.endsWith("ar") || word.endsWith("er") || word.endsWith("ir")) {
            return "verb";
        } else if (word.endsWith("mente")) {
            return "adverb";
        } else if (word.endsWith("ción") || word.endsWith("dad") || word.endsWith("ismo")) {
            return "noun";
        } else {
            return "other";
        }
    }

    public List<Word> getAllWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    // Set image URL
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getImageUrl(word.getWord()));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    private String getImageUrl(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return "";
        }

        // Kelimeyi temizle ve URL-safe yap
        String cleanKeyword = keyword.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .replace(" ", "-");

        // Picsum Photos API - performanslı ve ücretsiz
        // Her kelime için sabit bir seed kullanarak aynı resmi döndür
        int seed = Math.abs(cleanKeyword.hashCode() % 1000);
        return "https://picsum.photos/seed/" + seed + "/400/300";
    }

    public List<Word> getUnknownWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Firebase doesn't support != so we'll filter in memory
        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    return word;
                })
                .filter(word -> word.getCorrectCount() < 3)
                .map(word -> {
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getImageUrl(word.getWord()));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public List<Word> getNewWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    return word;
                })
                .filter(word -> word.getStudyCount() == 0)
                .limit(10)
                .map(word -> {
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getImageUrl(word.getWord()));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public Word updateWordProgress(String language, String wordId, boolean isCorrect)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        DocumentReference docRef = firestore.collection(collectionName).document(wordId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastStudyDate", new Date());
        updates.put("studyCount", FieldValue.increment(1));

        if (isCorrect) {
            updates.put("correctCount", FieldValue.increment(1));
        } else {
            updates.put("incorrectCount", FieldValue.increment(1));
        }

        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get();

        DocumentSnapshot document = docRef.get().get();
        Word word = document.toObject(Word.class);
        word.setId(document.getId());
        return word;
    }

    public Word addWord(String language, Word word) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Varsayılan değerleri ayarla
        word.setCorrectCount(0);
        word.setIncorrectCount(0);
        word.setStudyCount(0);
        word.setLastStudyDate(new Date());
        word.setFavorite(false);

        if (word.getDifficulty() == null) {
            word.setDifficulty("medium");
        }
        if (word.getCategory() == null) {
            word.setCategory("other");
        }
        if (word.getTags() == null || word.getTags().isEmpty()) {
            word.setTags(Arrays.asList("general"));
        }
        if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
            word.setImageUrl(getImageUrl(word.getWord()));
        }

        ApiFuture<DocumentReference> future = firestore.collection(collectionName).add(word);
        DocumentReference docRef = future.get();
        word.setId(docRef.getId());

        return word;
    }

    public Word updateWord(String language, String wordId, Word word)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        DocumentReference docRef = firestore.collection(collectionName).document(wordId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("word", word.getWord());
        updates.put("translation", word.getTranslation());
        updates.put("category", word.getCategory());
        updates.put("difficulty", word.getDifficulty());
        updates.put("example", word.getExample());
        updates.put("pronunciation", word.getPronunciation());
        updates.put("tags", word.getTags());

        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get();

        DocumentSnapshot document = docRef.get().get();
        Word updatedWord = document.toObject(Word.class);
        updatedWord.setId(document.getId());
        return updatedWord;
    }

    public void deleteWord(String language, String wordId)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        DocumentReference docRef = firestore.collection(collectionName).document(wordId);
        ApiFuture<WriteResult> future = docRef.delete();
        future.get();
    }

    public Word toggleFavorite(String language, String wordId)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        DocumentReference docRef = firestore.collection(collectionName).document(wordId);
        DocumentSnapshot document = docRef.get().get();

        Boolean currentFavorite = document.getBoolean("isFavorite");
        if (currentFavorite == null) currentFavorite = false;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", !currentFavorite);

        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get();

        document = docRef.get().get();
        Word word = document.toObject(Word.class);
        word.setId(document.getId());
        return word;
    }

    public List<Word> getFavoriteWords(String language)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        Query query = firestore.collection(collectionName)
                .whereEqualTo("isFavorite", true);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getImageUrl(word.getWord()));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public List<Word> getQuizWords(String language, int count)
            throws ExecutionException, InterruptedException {
        List<Word> allWords = getAllWords(language);
        Collections.shuffle(allWords);
        return allWords.stream().limit(count).collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics(String language)
            throws ExecutionException, InterruptedException {
        List<Word> allWords = getAllWords(language);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allWords.size());
        stats.put("learned", allWords.stream().filter(w -> w.getCorrectCount() >= 5).count());
        stats.put("learning", allWords.stream()
                .filter(w -> w.getCorrectCount() > 0 && w.getCorrectCount() < 5).count());
        stats.put("unknown", allWords.stream().filter(w -> w.getCorrectCount() == 0).count());
        stats.put("favorites", allWords.stream().filter(Word::isFavorite).count());

        return stats;
    }
}