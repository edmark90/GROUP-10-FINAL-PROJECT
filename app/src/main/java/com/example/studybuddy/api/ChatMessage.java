package com.example.studybuddy.api;

public class ChatMessage {
    public String role;
    public String content;
    public String[] options;
    
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.options = null;
    }
    
    public ChatMessage(String role, String content, String[] options) {
        this.role = role;
        this.content = content;
        this.options = options;
    }
    
    public boolean hasOptions() {
        return options != null && options.length > 0;
    }
}
