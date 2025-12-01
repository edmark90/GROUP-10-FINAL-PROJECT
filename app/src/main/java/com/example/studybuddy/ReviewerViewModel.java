package com.example.studybuddy;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.studybuddy.api.ChatMessage;
import com.example.studybuddy.api.GroqApiService;
import com.example.studybuddy.api.QuizResult;
import com.example.studybuddy.data.AppDatabase;
import com.example.studybuddy.data.CategoryPerformance;
import com.example.studybuddy.data.QuizHistory;
import com.example.studybuddy.data.QuizHistoryDao;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewerViewModel extends AndroidViewModel {
    private AppDatabase database;
    private QuizHistoryDao dao;
    private GroqApiService groqApiService;
    private ReviewerService reviewerService;
    private ExecutorService executorService;
    
    public ReviewSession session = new ReviewSession();
    
    private MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    
    // Hardcoded API key
    private static final String GROQ_API_KEY = "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";
    
    public ReviewerViewModel(Application application) {
        super(application);
        database = AppDatabase.getDatabase(application);
        dao = database.quizHistoryDao();
        executorService = Executors.newSingleThreadExecutor();
        

        groqApiService = GroqApiService.Factory.create(GROQ_API_KEY);
        reviewerService = new ReviewerService(groqApiService);
    }
    
    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public void startSession() {
        executorService.execute(() -> {
            isLoading.postValue(true);
            try {
                String greeting = reviewerService.getGreeting();
                List<ChatMessage> initialMessages = new ArrayList<>();
                initialMessages.add(new ChatMessage("assistant", greeting));
                messages.postValue(initialMessages);
                session.state = SessionState.WAITING_FOR_COMMAND;
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("Chain validation failed")) {
                    error.postValue("Secure connection failed. Please check your internet connection and device date/time, then try again.");
                } else {
                    error.postValue("Error: " + message);
                }
                android.util.Log.e("ReviewerViewModel", "Error starting session", e);
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    public void sendMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }
        
        executorService.execute(() -> {
            isLoading.postValue(true);
            
            // Add user message to UI
            List<ChatMessage> currentMessages = messages.getValue();
            if (currentMessages == null) {
                currentMessages = new ArrayList<>();
            } else {
                currentMessages = new ArrayList<>(currentMessages);
            }
            currentMessages.add(new ChatMessage("user", userMessage));
            messages.postValue(currentMessages);
            
            try {
                ReviewerResponse response = reviewerService.processUserMessage(userMessage, session);
                
                // Handle weak topics request
                if (response.shouldRequestWeakTopics) {
                    loadWeakTopics();
                    return;
                }
                

                List<ChatMessage> updatedMessages = messages.getValue();
                if (updatedMessages == null) {
                    updatedMessages = new ArrayList<>();
                } else {
                    updatedMessages = new ArrayList<>(updatedMessages);
                }
                
                // Add options if this is a question
                ChatMessage aiMessage;
                if (session.state == SessionState.WAITING_FOR_ANSWER && session.currentOptions != null) {
                    aiMessage = new ChatMessage("assistant", response.text, session.currentOptions);
                } else {
                    aiMessage = new ChatMessage("assistant", response.text);
                }
                updatedMessages.add(aiMessage);
                messages.postValue(updatedMessages);
                

                if (response.quizResult != null) {
                    saveQuizResult(response.quizResult);
                }
                

                if (session.state == SessionState.GENERATING_QUESTION && 
                    session.currentQuestionIndex < session.questionCount) {
                    generateNextQuestion();
                }
                
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("Chain validation failed")) {
                    error.postValue("Secure connection failed. Please check your internet connection and device date/time, then try again.");
                } else {
                    error.postValue("Error: " + message);
                }
                android.util.Log.e("ReviewerViewModel", "Error sending message", e);
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    private void generateNextQuestion() {
        executorService.execute(() -> {
            try {
                ReviewerResponse response;
                if (session.weakTopics != null && session.weakQuestions != null && 
                    !session.weakQuestions.isEmpty()) {
                    response = reviewerService.generateWeakTopicQuestion(session, session.weakQuestions);
                } else {
                    response = reviewerService.processUserMessage("generate_next_question", session);
                }
                
                List<ChatMessage> updatedMessages = messages.getValue();
                if (updatedMessages == null) {
                    updatedMessages = new ArrayList<>();
                } else {
                    updatedMessages = new ArrayList<>(updatedMessages);
                }
                // Add options if this is a question
                ChatMessage aiMessage;
                if (session.state == SessionState.WAITING_FOR_ANSWER && session.currentOptions != null) {
                    aiMessage = new ChatMessage("assistant", response.text, session.currentOptions);
                } else {
                    aiMessage = new ChatMessage("assistant", response.text);
                }
                updatedMessages.add(aiMessage);
                messages.postValue(updatedMessages);
                
                // Save quiz result if available
                if (response.quizResult != null) {
                    saveQuizResult(response.quizResult);
                }
            } catch (Exception e) {
                error.postValue("Error generating question: " + e.getMessage());
            }
        });
    }
    
    private void loadWeakTopics() {
        executorService.execute(() -> {
            try {
                List<CategoryPerformance> performance = dao.getCategoryPerformance();
                if (performance == null) {
                    performance = new ArrayList<>();
                }
                
                List<String> weakCategories = new ArrayList<>();
                
                for (CategoryPerformance perf : performance) {
                    if (perf != null && perf.category != null && perf.incorrect > 0 && 
                        perf.total > 0 && ((double) perf.incorrect / perf.total) > 0.3) {
                        weakCategories.add(perf.category);
                    }
                }
                
                if (weakCategories.isEmpty()) {
                    List<ChatMessage> updatedMessages = messages.getValue();
                    if (updatedMessages == null) {
                        updatedMessages = new ArrayList<>();
                    } else {
                        updatedMessages = new ArrayList<>(updatedMessages);
                    }
                    updatedMessages.add(new ChatMessage("assistant", 
                        "Great news! You don't have any weak topics yet, or you haven't taken enough quizzes. Try taking a regular quiz first!"));
                    messages.postValue(updatedMessages);
                    session.state = SessionState.WAITING_FOR_COMMAND;
                    return;
                }
                
                session.weakTopics = weakCategories;
                List<QuizHistory> weakQuestions = dao.getWeakQuestions(weakCategories, 20);
                session.weakQuestions = weakQuestions;
                
                session.subject = String.join(", ", weakCategories);
                session.questionCount = Math.min(weakQuestions.size(), 10);
                session.currentQuestionIndex = 0;
                session.state = SessionState.GENERATING_QUESTION;
                
                generateNextQuestion();
                
            } catch (Exception e) {
                error.postValue("Error loading weak topics: " + e.getMessage());
            }
        });
    }
    
    private void saveQuizResult(QuizResult result) {
        executorService.execute(() -> {
            try {
                if (result == null) {
                    android.util.Log.w("ReviewerViewModel", "Attempted to save null quiz result");
                    return;
                }
                
                QuizHistory quizHistory = new QuizHistory(
                    result.category != null ? result.category : "Unknown",
                    result.question != null ? result.question : "",
                    result.userAnswer != null ? result.userAnswer : "",
                    result.correctAnswer != null ? result.correctAnswer : "",
                    result.isCorrect,
                    result.explanation != null ? result.explanation : ""
                );
                
                // Save options as JSON string
                if (result.options != null && result.options.length > 0) {
                    quizHistory.options = new com.google.gson.Gson().toJson(result.options);
                }
                
                // Save session ID
                quizHistory.sessionId = result.sessionId != null ? result.sessionId : session.sessionId;
                
                dao.insert(quizHistory);
                android.util.Log.d("ReviewerViewModel", "Quiz result saved successfully");
            } catch (Exception e) {
                android.util.Log.e("ReviewerViewModel", "Error saving quiz result", e);
                error.postValue("Error saving quiz result: " + e.getMessage());
            }
        });
    }
    
    public void clearError() {
        error.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}

