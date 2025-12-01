package com.example.studybuddy.api;

import com.google.gson.annotations.SerializedName;

public class QuizResult {
    public String category;
    @SerializedName("question")
    public String question;
    @SerializedName("user_answer")
    public String userAnswer;
    @SerializedName("correct_answer")
    public String correctAnswer;
    @SerializedName("is_correct")
    public boolean isCorrect;
    public String explanation;
    public String[] options;
    public String sessionId;
}
