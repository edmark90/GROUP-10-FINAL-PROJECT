package com.example.studybuddy;

import com.example.studybuddy.api.ChatMessage;
import com.example.studybuddy.data.QuizHistory;
import java.util.ArrayList;
import java.util.List;

public class ReviewSession {
    public SessionState state = SessionState.GREETING;
    public String subject;
    public int questionCount = 0;
    public int currentQuestionIndex = 0;
    public String currentQuestion;
    public List<ChatMessage> conversationHistory = new ArrayList<>();
    public List<String> weakTopics;
    public List<QuizHistory> weakQuestions;
    public String sessionId;
    public String[] currentOptions;
    public List<String> askedQuestions = new ArrayList<>();
    
    public void reset() {
        state = SessionState.GREETING;
        subject = null;
        questionCount = 0;
        currentQuestionIndex = 0;
        currentQuestion = null;
        conversationHistory.clear();
        weakTopics = null;
        weakQuestions = null;
        sessionId = null;
        currentOptions = null;
        askedQuestions.clear();
    }
}

enum SessionState {
    GREETING,
    WAITING_FOR_COMMAND,
    ASKING_QUESTION_COUNT,
    GENERATING_QUESTION,
    WAITING_FOR_ANSWER,

}

