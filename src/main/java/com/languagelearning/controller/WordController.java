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
public class WordController {

    @Autowired
    private WordService wordService;

    @PostConstruct
    public void init() {
        System.out.println("WordController initialized!");
        // Uygulama başladığında mevcut kelimeleri güncelle
        try {
            wordService.migrateExistingWords("es");
            wordService.migrateExistingWords("en");
            System.out.println("Migration completed!");
        } catch (Exception e) {
            System.err.println("Migration error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "API is working!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{language}")
    public ResponseEntity<List<Word>> getAllWords(@PathVariable String language) {
        System.out.println("Getting all words for language: " + language);
        try {
            List<Word> words = wordService.getAllWords(language);
            System.out.println("Found " + words.size() + " words");
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{language}/unknown")
    public ResponseEntity<List<Word>> getUnknownWords(@PathVariable String language) {
        System.out.println("Getting unknown words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getUnknownWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{language}/new")
    public ResponseEntity<List<Word>> getNewWords(@PathVariable String language) {
        System.out.println("Getting new words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getNewWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{language}/favorites")
    public ResponseEntity<List<Word>> getFavoriteWords(@PathVariable String language) {
        try {
            return ResponseEntity.ok(wordService.getFavoriteWords(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{language}/quiz")
    public ResponseEntity<List<Word>> getQuizWords(
            @PathVariable String language,
            @RequestParam(defaultValue = "10") int count) {
        System.out.println("Getting quiz words for language: " + language);
        try {
            return ResponseEntity.ok(wordService.getQuizWords(language, count));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{language}/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String language) {
        try {
            return ResponseEntity.ok(wordService.getStatistics(language));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{language}")
    public ResponseEntity<Word> addWord(@PathVariable String language, @RequestBody Word word) {
        System.out.println("Adding new word: " + word.getWord());
        try {
            return ResponseEntity.ok(wordService.addWord(language, word));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{language}/{wordId}")
    public ResponseEntity<Word> updateWord(
            @PathVariable String language,
            @PathVariable String wordId,
            @RequestBody Word word) {
        System.out.println("Updating word: " + wordId);
        try {
            return ResponseEntity.ok(wordService.updateWord(language, wordId, word));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{language}/{wordId}")
    public ResponseEntity<Void> deleteWord(
            @PathVariable String language,
            @PathVariable String wordId) {
        System.out.println("Deleting word: " + wordId);
        try {
            wordService.deleteWord(language, wordId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{language}/{wordId}/progress")
    public ResponseEntity<Word> updateProgress(
            @PathVariable String language,
            @PathVariable String wordId,
            @RequestBody Map<String, Boolean> body) {
        System.out.println("Updating progress for word: " + wordId);
        try {
            boolean isCorrect = body.get("correct");
            return ResponseEntity.ok(wordService.updateWordProgress(language, wordId, isCorrect));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{language}/{wordId}/favorite")
    public ResponseEntity<Word> toggleFavorite(
            @PathVariable String language,
            @PathVariable String wordId) {
        System.out.println("Toggling favorite for word: " + wordId);
        try {
            return ResponseEntity.ok(wordService.toggleFavorite(language, wordId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{language}/migrate")
    public ResponseEntity<Map<String, String>> migrateWords(@PathVariable String language) {
        System.out.println("Migrating words for language: " + language);
        try {
            wordService.migrateExistingWords(language);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Migration completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}