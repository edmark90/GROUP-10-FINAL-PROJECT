package com.example.studybuddy.api;

import com.google.gson.annotations.SerializedName;

public class Choice {
    public ChatMessage message;
    @SerializedName("finish_reason")
    public String finishReason;
}

