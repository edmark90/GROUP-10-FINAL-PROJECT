package com.example.studybuddy.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GroqChatRequest {
    public List<ChatMessage> messages;
    public String model = "llama-3.3-70b-versatile";
    public double temperature = 0.7;
    @SerializedName("max_tokens")
    public int maxTokens = 1024;
    
    public GroqChatRequest(List<ChatMessage> messages) {
        this.messages = messages;
    }
}
