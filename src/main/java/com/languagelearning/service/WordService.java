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
    private static final int PAGE_SIZE = 20; // Sayfa başına kelime sayısı
    private static final String UNSPLASH_ACCESS_KEY = "YOUR_UNSPLASH_ACCESS_KEY"; // Buraya Unsplash API key'inizi ekleyin

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
            ApiFuture<QuerySnapshot> future = firestore.collection("test").limit(1).get();
            future.get();
            System.out.println("Firestore connection test successful");
        } catch (Exception e) {
            System.err.println("Firestore connection test failed: " + e.getMessage());
            throw e;
        }
    }

    private void checkFirestore() {
        if (firestore == null) {
            throw new RuntimeException("Firestore connection is not available. Please check your Firebase configuration.");
        }
    }

    // Pagination ile kelime getirme
    public Map<String, Object> getWordsPaginated(String language, String lastWordId, int limit)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        Query query = firestore.collection(collectionName)
                .orderBy(FieldPath.documentId())
                .limit(Math.min(limit, PAGE_SIZE));

        if (lastWordId != null && !lastWordId.isEmpty()) {
            DocumentSnapshot lastDoc = firestore.collection(collectionName)
                    .document(lastWordId).get().get();
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc);
            }
        }

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Word> words = documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("words", words);
        result.put("hasMore", documents.size() == limit);
        result.put("lastWordId", words.isEmpty() ? null : words.get(words.size() - 1).getId());

        return result;
    }

    // Geliştirilmiş görsel URL'si - kelimeyle alakalı
    private String getEnhancedImageUrl(String keyword, String language) {
        if (keyword == null || keyword.isEmpty()) {
            return "";
        }

        // Kelimeyi temizle
        String cleanKeyword = keyword.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim();

        // Dil bazlı çeviri ekle (basit bir mapping)
        String searchTerm = cleanKeyword;
        if (language.equals("es")) {
            searchTerm = translateSpanishToEnglish(cleanKeyword);
        }

        // Unsplash API kullan (ücretsiz limit: 50 req/hour)
        // Alternatif olarak Pexels API de kullanılabilir
        return String.format(
                "https://source.unsplash.com/400x300/?%s,language,education",
                searchTerm.replace(" ", ",")
        );
    }

    // Basit İspanyolca-İngilizce çeviri mapping'i
    private String translateSpanishToEnglish(String spanish) {
        Map<String, String> commonTranslations = new HashMap<>();
        commonTranslations.put("casa", "house");
        commonTranslations.put("perro", "dog");
        commonTranslations.put("gato", "cat");
        commonTranslations.put("agua", "water");
        commonTranslations.put("comida", "food");
        commonTranslations.put("libro", "book");
        commonTranslations.put("coche", "car");
        commonTranslations.put("escuela", "school");
        commonTranslations.put("amigo", "friend");
        commonTranslations.put("familia", "family");
        // Daha fazla kelime eklenebilir

        return commonTranslations.getOrDefault(spanish, spanish);
    }

    // Migration fonksiyonu güncellendi
    public void migrateExistingWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Batch olarak al, tüm verileri bir anda çekme
        Query query = firestore.collection(collectionName).limit(100);
        ApiFuture<QuerySnapshot> future = query.get();
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
            // Yeni görsel URL'si ekle
            if (!data.containsKey("imageUrl") || data.get("imageUrl") == null) {
                updates.put("imageUrl", getEnhancedImageUrl(data.get("word").toString(), language));
            }

            if (!updates.isEmpty()) {
                batch.update(doc.getReference(), updates);
                batchCount++;

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

    // Optimized getAllWords - limit ile
    public List<Word> getAllWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Tüm kelimeleri çekme, maksimum 100 kelime
        Query query = firestore.collection(collectionName).limit(100);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    // Lazy loading için yeni method
    public List<Word> getWordsLazy(String language, int offset, int limit)
            throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        Query query = firestore.collection(collectionName)
                .orderBy("word")
                .offset(offset)
                .limit(Math.min(limit, PAGE_SIZE));

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public List<Word> getUnknownWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Limit ekle performans için
        Query query = firestore.collection(collectionName).limit(50);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    return word;
                })
                .filter(word -> word.getCorrectCount() < 3)
                .limit(20) // Maksimum 20 kelime döndür
                .map(word -> {
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public List<Word> getNewWords(String language) throws ExecutionException, InterruptedException {
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        Query query = firestore.collection(collectionName)
                .whereEqualTo("studyCount", 0)
                .limit(20); // Direkt Firestore'da limit

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
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
            word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
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
                .whereEqualTo("isFavorite", true)
                .limit(30); // Limit ekle

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public List<Word> getQuizWords(String language, int count)
            throws ExecutionException, InterruptedException {
        // Rastgele kelimeler için optimizasyon
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Önce toplam kelime sayısını al
        Query countQuery = firestore.collection(collectionName).limit(200);
        ApiFuture<QuerySnapshot> future = countQuery.get();
        List<QueryDocumentSnapshot> allDocs = future.get().getDocuments();

        // Firestore'dan dönen immutable listeyi mutable ArrayList'e kopyala
        List<QueryDocumentSnapshot> mutableDocs = new ArrayList<>(allDocs);

        // Şimdi güvenle shuffle yapabiliriz
        Collections.shuffle(mutableDocs);

        return mutableDocs.stream()
                .limit(count)
                .map(doc -> {
                    Word word = doc.toObject(Word.class);
                    word.setId(doc.getId());
                    if (word.getImageUrl() == null || word.getImageUrl().isEmpty()) {
                        word.setImageUrl(getEnhancedImageUrl(word.getWord(), language));
                    }
                    return word;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics(String language)
            throws ExecutionException, InterruptedException {
        // İstatistikler için optimize edilmiş sorgular
        checkFirestore();
        String collectionName = language.equals("en") ? "englishWords" : "spanishWords";

        // Toplam sayı için aggregation kullan (daha performanslı)
        AggregateQuery countQuery = firestore.collection(collectionName).count();
        AggregateQuerySnapshot countSnapshot = countQuery.get().get();
        long totalCount = countSnapshot.getCount();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);

        // Diğer istatistikler için sample al
        Query sampleQuery = firestore.collection(collectionName).limit(100);
        ApiFuture<QuerySnapshot> future = sampleQuery.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Word> sampleWords = documents.stream()
                .map(doc -> doc.toObject(Word.class))
                .collect(Collectors.toList());

        // Sample üzerinden tahmin yap
        long learned = sampleWords.stream().filter(w -> w.getCorrectCount() >= 5).count();
        long learning = sampleWords.stream()
                .filter(w -> w.getCorrectCount() > 0 && w.getCorrectCount() < 5).count();
        long unknown = sampleWords.stream().filter(w -> w.getCorrectCount() == 0).count();
        long favorites = sampleWords.stream().filter(Word::isFavorite).count();

        // Oransal hesapla
        double ratio = totalCount / (double) sampleWords.size();
        stats.put("learned", Math.round(learned * ratio));
        stats.put("learning", Math.round(learning * ratio));
        stats.put("unknown", Math.round(unknown * ratio));
        stats.put("favorites", Math.round(favorites * ratio));

        return stats;
    }
}