package com.example.studybuddy;

import com.example.studybuddy.api.QuizResult;

public class ReviewerResponse {
    public String text;
    public QuizResult quizResult;
    public boolean shouldRequestWeakTopics;
    
    public ReviewerResponse(String text, QuizResult quizResult, boolean shouldRequestWeakTopics) {
        this.text = text;
        this.quizResult = quizResult;
        this.shouldRequestWeakTopics = shouldRequestWeakTopics;
    }
}

