package com.example.studybuddy.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "quiz_history")
public class QuizHistory {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    
    public String category;
    public String question;
    public String userAnswer;
    public String correctAnswer;
    public boolean isCorrect;
    public String explanation;
    public long timestamp;
    public int reviewCount;
    public long lastReviewed;
    public String options;
    public String sessionId;
    public QuizHistory() {
        this.timestamp = System.currentTimeMillis();
        this.lastReviewed = System.currentTimeMillis();
        this.reviewCount = 0;
    }
    
    @Ignore
    public QuizHistory(String category, String question, String userAnswer, 
                      String correctAnswer, boolean isCorrect, String explanation) {
        this.category = category;
        this.question = question;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.explanation = explanation;
        this.timestamp = System.currentTimeMillis();
        this.lastReviewed = System.currentTimeMillis();
        this.reviewCount = 0;
    }
}

