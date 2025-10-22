package com.languagelearning.controller;

import com.languagelearning.model.Word;
import com.languagelearning.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/words")
@CrossOrigin(origins = "*")
public class WordController {

    @Autowired
    private WordService wordService;

    @PostConstruct
    public void init() {
        System.out.println("WordController initialized!");
        System.out.println("Auto-migration disabled. Use /api/words/{language}/migrate endpoint manually.");
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "API is working!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        try {
            wordService.testConnection();
            response.put("status", "healthy");
            response.put("firebase", "connected");
        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("firebase", "disconnected");
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // Pagination endpoint'i ekle
    @GetMapping("/{language}/paginated")
    public ResponseEntity<?> getWordsPaginated(
            @PathVariable String language,
            @RequestParam(required = false) String lastWordId,
            @RequestParam(defaultValue = "20") int limit) {
        System.out.println("Getting paginated words for language: " + language);
        try {
            Map<String, Object> result = wordService.getWordsPaginated(language, lastWordId, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error getting paginated words: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Lazy loading endpoint'i
    @GetMapping("/{language}/lazy")
    public ResponseEntity<?> getWordsLazy(
            @PathVariable String language,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        System.out.println("Getting lazy loaded words for language: " + language);
        try {
            List<Word> words = wordService.getWordsLazy(language, offset, limit);
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            System.err.println("Error getting lazy words: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{language}")
    public ResponseEntity<?> getAllWords(@PathVariable String language) {
        System.out.println("Getting all words for language: " + language);
        try {
            List<Word> words = wordService.getAllWords(language);
            System.out.println("Found " + words.size() + " words");
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            System.err.println("Error getting words: " + e.getMessage());
            e.printStackTrace();

            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch words");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{language}/unknown")
    public ResponseEntity<?> getUnknownWords(@PathVariable String language) {
        System.out.println("Getting unknown words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getUnknownWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{language}/new")
    public ResponseEntity<?> getNewWords(@PathVariable String language) {
        System.out.println("Getting new words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getNewWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{language}/favorites")
    public ResponseEntity<?> getFavoriteWords(@PathVariable String language) {
        try {
            return ResponseEntity.ok(wordService.getFavoriteWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{language}/quiz")
    public ResponseEntity<?> getQuizWords(
            @PathVariable String language,
            @RequestParam(defaultValue = "10") int count) {
        System.out.println("Getting quiz words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getQuizWords(language, count));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{language}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable String language) {
        try {
            return ResponseEntity.ok(wordService.getStatistics(language));
        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("total", 0);
            defaultStats.put("learned", 0);
            defaultStats.put("learning", 0);
            defaultStats.put("unknown", 0);
            defaultStats.put("favorites", 0);
            defaultStats.put("error", true);
            defaultStats.put("message", "Unable to fetch statistics");

            return ResponseEntity.ok(defaultStats);
        }
    }

    @PostMapping("/{language}")
    public ResponseEntity<?> addWord(@PathVariable String language, @RequestBody Word word) {
        System.out.println("Adding new word: " + word.getWord());
        try {
            return ResponseEntity.ok(wordService.addWord(language, word));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{language}/{wordId}")
    public ResponseEntity<?> updateWord(
            @PathVariable String language,
            @PathVariable String wordId,
            @RequestBody Word word) {
        System.out.println("Updating word: " + wordId);
        try {
            return ResponseEntity.ok(wordService.updateWord(language, wordId, word));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{language}/{wordId}")
    public ResponseEntity<?> deleteWord(
            @PathVariable String language,
            @PathVariable String wordId) {
        System.out.println("Deleting word: " + wordId);
        try {
            wordService.deleteWord(language, wordId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{language}/{wordId}/progress")
    public ResponseEntity<?> updateProgress(
            @PathVariable String language,
            @PathVariable String wordId,
            @RequestBody Map<String, Boolean> body) {
        System.out.println("Updating progress for word: " + wordId);
        try {
            boolean isCorrect = body.get("correct");
            return ResponseEntity.ok(wordService.updateWordProgress(language, wordId, isCorrect));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{language}/{wordId}/favorite")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable String language,
            @PathVariable String wordId) {
        System.out.println("Toggling favorite for word: " + wordId);
        try {
            return ResponseEntity.ok(wordService.toggleFavorite(language, wordId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}