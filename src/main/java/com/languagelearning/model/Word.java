package com.languagelearning.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Word {
    private String id;
    private String word;
    private String translation;
    private int correctCount;
    private int incorrectCount;
    private Date lastStudyDate;
    private String difficulty; // easy, medium, hard
    private String category; // verb, noun, adjective, etc.
    private String example; // örnek cümle
    private String pronunciation; // fonetik yazılış
    private boolean isFavorite;
    private int studyCount;
    private List<String> tags;
    private String imageUrl;
    private String audioUrl;
}