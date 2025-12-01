package com.example.studybuddy;

import com.example.studybuddy.api.*;
import com.example.studybuddy.data.QuizHistory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ReviewerService {
    // ... (rest of the code remains the same)
    private GroqApiService groqApiService;
    
    private static final String SYSTEM_PROMPT = 
        "You are StudyBuddy, an AI reviewer system designed to help students study. Your role is to:\n\n" +
        "1. When the user opens the reviewer, greet them warmly and wait for commands.\n" +
        "2. When they say \"Quiz me about [subject]\", ask how many questions they want, then generate that number of questions one at a time.\n" +
        "3. Wait for the user's answer before continuing to the next question.\n" +
        "4. Check if their answer is correct, give short and simple explanations (1-2 sentences max).\n" +
        "5. ALWAYS output a JSON block after each answer check with this exact format:\n" +
        "{\n" +
        "    \"category\": \"[subject/category]\",\n" +
        "    \"question\": \"[the question asked]\",\n" +
        "    \"user_answer\": \"[user's answer]\",\n" +
        "    \"correct_answer\": \"[correct answer]\",\n" +
        "    \"is_correct\": true/false,\n" +
        "    \"explanation\": \"[brief explanation]\"\n" +
        "}\n" +
        "You must NEVER skip the JSON block - it's critical for the app to save the data.\n\n" +
        "6. When the user requests personalized review (like \"AP reviewer\", \"review my weak topics\", \"review my mistakes\", or \"focus on weak areas\"), respond with exactly: REQUEST_WEAK_TOPICS\n\n" +
        "7. When reviewing weak topics, generate targeted questions only from categories the user is weak in, prioritizing questions they previously answered incorrectly using spaced repetition.\n\n" +
        "8. Throughout the entire interaction:\n" +
        "   - Remain friendly, brief, and motivating\n" +
        "   - Never continue without the user's input\n" +
        "   - Never give answers early\n" +
        "   - Always follow the exact JSON format\n" +
        "   - Keep explanations concise (1-2 sentences)";
    
    public ReviewerService(GroqApiService groqApiService) {
        this.groqApiService = groqApiService;
    }

    public String getGreeting() throws IOException {
        return "Hi! I'm StudyBuddy 👋\n\n" +
                "Start with: 'Quiz me about [topic]' or type any subject.\n" +
                "I'll generate and check your quiz automatically!";
    }
    
    public ReviewerResponse processUserMessage(String userMessage, ReviewSession session) throws IOException {
        // Simple greetings: repeat the introduction instead of starting a quiz
        String trimmedLower = userMessage.toLowerCase().trim();
        if (session.state == SessionState.WAITING_FOR_COMMAND &&
                ("hi".equals(trimmedLower) || "hello".equals(trimmedLower) || "hey".equals(trimmedLower))) {
            String greeting = getGreeting();
            return new ReviewerResponse(greeting, null, false);
        }

        // Check for weak topic requests
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("ap reviewer") || 
            lowerMessage.contains("review my weak topics") ||
            lowerMessage.contains("review my mistakes") ||
            lowerMessage.contains("focus on weak areas") ||
            lowerMessage.contains("weak topics")) {
            return new ReviewerResponse("REQUEST_WEAK_TOPICS", null, true);
        }
        
        // Build conversation history
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.addAll(session.conversationHistory);
        messages.add(new ChatMessage("user", userMessage));
        
        // Handle quiz me command - optimized for faster response
        if (session.state == SessionState.WAITING_FOR_COMMAND) {
            Pattern quizPattern = Pattern.compile("quiz me about (.+)", Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = quizPattern.matcher(userMessage);
            if (matcher.find()) {
                session.subject = matcher.group(1).trim();
                session.state = SessionState.ASKING_QUESTION_COUNT;

                return new ReviewerResponse(
                    "Great! How many questions would you like about " + session.subject + "?",
                    null, false
                );
            } else {

                String trimmed = userMessage.trim();
                if (!trimmed.isEmpty()) {
                    session.subject = trimmed;
                    session.state = SessionState.ASKING_QUESTION_COUNT;
                    return new ReviewerResponse(
                        "Great! How many questions would you like about " + session.subject + "?",
                        null, false
                    );
                }
            }
        }
        
        // Handle question count
        if (session.state == SessionState.ASKING_QUESTION_COUNT) {
            try {
                int count = Integer.parseInt(userMessage.trim());
                if (count > 0) {
                    session.questionCount = count;
                    session.currentQuestionIndex = 0;
                    session.state = SessionState.GENERATING_QUESTION;
                    return generateQuestion(session);
                }
            } catch (NumberFormatException e) {
                // Not a number
            }
            return new ReviewerResponse(
                "Please enter a valid number of questions (e.g., 5, 10).",
                null, false
            );
        }
        
        // Handle answer submission
        if (session.state == SessionState.WAITING_FOR_ANSWER) {
            return checkAnswer(userMessage, session);
        }

        if (session.state == SessionState.GENERATING_QUESTION && userMessage.equals("generate_next_question")) {
            return generateQuestion(session);
        }
        

        GroqChatRequest request = new GroqChatRequest(messages);
        request.maxTokens = 512;
        
        retrofit2.Response<GroqChatResponse> response = groqApiService.chatCompletion(request).execute();
        
        String aiResponse = "I'm here to help! Try saying 'Quiz me about [subject]' to start.";
        if (response.isSuccessful() && response.body() != null && 
            response.body().choices != null && !response.body().choices.isEmpty()) {
            aiResponse = response.body().choices.get(0).message.content;
        }
        
        session.conversationHistory.add(new ChatMessage("user", userMessage));
        session.conversationHistory.add(new ChatMessage("assistant", aiResponse));
        
        return new ReviewerResponse(aiResponse, null, false);
    }
    
    private ReviewerResponse generateQuestion(ReviewSession session) throws IOException {
        // Generate a new session ID for the first question of each quiz
        if (session.currentQuestionIndex == 0 || session.sessionId == null || session.sessionId.isEmpty()) {
            session.sessionId = "session_" + System.currentTimeMillis();
        }
        
        String context;
        if (session.weakTopics != null && session.weakQuestions != null) {
            context = "Create ONE multiple-choice question to help the student practice weak topics about " + session.subject + ".";
        } else {
            context = "Create ONE multiple-choice question about " + session.subject +
                     " (Question " + (session.currentQuestionIndex + 1) + "/" + session.questionCount + ").";
        }
        
        // Use minimal conversation history for faster response
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(
                "system",
                "You are a question generator for a quiz app. " +
                "Respond ONLY with valid JSON, no extra text, in this exact format:\n" +
                "{\n" +
                "  \"question\": \"...\",\n" +
                "  \"options\": [\"...\", \"...\", \"...\", \"...\"]\n" +
                "}\n" +
                "The options array must contain exactly 4 short answer choices. Do not label them A/B/C/D; just provide the text."
        ));

        // Build user prompt, including previously asked questions so they are not repeated
        StringBuilder userPromptBuilder = new StringBuilder(context);
        if (session.askedQuestions != null && !session.askedQuestions.isEmpty()) {
            userPromptBuilder.append("\n\nPreviously asked questions, do NOT repeat any of them:\n");
            for (String asked : session.askedQuestions) {
                if (asked != null && !asked.isEmpty()) {
                    userPromptBuilder.append("- ").append(asked).append("\n");
                }
            }
        }
        messages.add(new ChatMessage("user", userPromptBuilder.toString()));
        
        GroqChatRequest request = new GroqChatRequest(messages);
        request.maxTokens = 300;
        
        String questionText = "";
        String[] options = new String[4];
        
        try {
            retrofit2.Response<GroqChatResponse> response = groqApiService.chatCompletion(request).execute();

            if (!response.isSuccessful()) {
                String errorBody = null;
                try {
                    errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                } catch (Exception ignored) {}
                android.util.Log.e(
                        "ReviewerService",
                        "Groq question HTTP error: code=" + response.code() + ", errorBody=" + errorBody
                );
            } else if (response.body() != null &&
                    response.body().choices != null && !response.body().choices.isEmpty()) {
                String fullResponse = response.body().choices.get(0).message.content.trim();
                android.util.Log.d("ReviewerService", "Groq question raw response: " + fullResponse);

                try {
                    JsonObject json = JsonParser.parseString(fullResponse).getAsJsonObject();
                    if (json.has("question")) {
                        questionText = json.get("question").getAsString();
                    }
                    if (json.has("options")) {
                        JsonArray array = json.getAsJsonArray("options");
                        int count = Math.min(4, array.size());
                        for (int i = 0; i < count; i++) {
                            options[i] = array.get(i).getAsString();
                        }
                    }
                } catch (Exception parseException) {
                    // Fallback: handle a variety of plain-text formats if JSON parsing fails
                    String[] lines = fullResponse.split("\n");
                    StringBuilder questionBuilder = new StringBuilder();
                    java.util.List<String> optionList = new java.util.ArrayList<>();

                    for (String rawLine : lines) {
                        String line = rawLine.trim();
                        if (line.isEmpty()) continue;

                        // A) / A. style options
                        if (line.matches("^[A-D][\\).]\\s*.+")) {
                            optionList.add(line.substring(2).trim());
                            continue;
                        }

                        // Numbered list: 1) / 1.
                        if (line.matches("^[0-9]+[\\).]\\s*.+")) {
                            line = line.replaceFirst("^[0-9]+[\\).]\\s*", "").trim();
                            optionList.add(line);
                            continue;
                        }

                        // Bullet list: - option
                        if (line.startsWith("- ")) {
                            optionList.add(line.substring(2).trim());
                            continue;
                        }

                        // If we don't yet have a question, treat first non-empty line as question
                        if (questionBuilder.length() == 0) {
                            questionBuilder.append(line);
                        } else {
                            // If options already started, treat as another option; otherwise, extend question
                            if (optionList.isEmpty()) {
                                questionBuilder.append(" ").append(line);
                            } else {
                                optionList.add(line);
                            }
                        }
                    }

                    questionText = questionBuilder.toString();
                    int count = Math.min(4, optionList.size());
                    for (int i = 0; i < count; i++) {
                        options[i] = optionList.get(i);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReviewerService", "Error generating question from Groq API", e);
        }
        
        // Fallback if Groq response is unusable
        if (questionText.isEmpty() || options[0] == null || options[1] == null 
                || options[2] == null || options[3] == null) {
            android.util.Log.w(
                    "ReviewerService",
                    "Failed to parse question/options. question='" + questionText + "', " +
                            "options0=" + options[0] + ", options1=" + options[1] + 
                            ", options2=" + options[2] + ", options3=" + options[3]
            );
            session.state = SessionState.WAITING_FOR_COMMAND;
            return new ReviewerResponse(
                "I couldn't understand the quiz generated for this topic. Please try again or rephrase the subject.",
                null,
                false
            );
        }
        
        session.currentQuestion = questionText;
        session.currentOptions = options;
        session.state = SessionState.WAITING_FOR_ANSWER;

        if (session.askedQuestions != null) {
            session.askedQuestions.add(questionText);
        }
        
        // Format question with options for display
        StringBuilder displayQuestion = new StringBuilder(questionText + "\n\n");
        for (int i = 0; i < options.length; i++) {
            displayQuestion.append((char)('A' + i)).append(") ").append(options[i]).append("\n");
        }
        
        session.conversationHistory.add(new ChatMessage("assistant", displayQuestion.toString()));
        
        return new ReviewerResponse(displayQuestion.toString(), null, false);
    }
    
    private ReviewerResponse checkAnswer(String userAnswer, ReviewSession session) throws IOException {

        String optionsText = "";
        if (session.currentOptions != null && session.currentOptions.length > 0) {
            optionsText = "\nOptions:\n";
            for (int i = 0; i < session.currentOptions.length; i++) {
                optionsText += (char)('A' + i) + ") " + session.currentOptions[i] + "\n";
            }
        }

        // Ask Groq to check the answer and return JSON with correctness
        String checkPrompt = "Q: " + session.currentQuestion + optionsText +
                "\nUser selected: " + userAnswer +
                "\n\nCheck if the user's answer is correct. Reply with: Correct/Incorrect, the correct answer (from options), brief explanation (1 sentence), then JSON: " +
                "{\"category\":\"" + (session.subject != null ? session.subject : "General") +
                "\",\"question\":\"" + session.currentQuestion.replace("\"", "\\\"") +
                "\",\"user_answer\":\"" + userAnswer.replace("\"", "\\\"") +
                "\",\"correct_answer\":\"[correct option from A/B/C/D]\",\"is_correct\":true/false,\"explanation\":\"[explanation]\"}";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "You are a tutor. Check answers quickly and provide JSON. The correct_answer must be one of the options provided."));
        messages.add(new ChatMessage("user", checkPrompt));

        GroqChatRequest request = new GroqChatRequest(messages);
        request.maxTokens = 300;

        retrofit2.Response<GroqChatResponse> response =
                groqApiService.chatCompletion(request).execute();

        String aiResponse = "";
        if (response.isSuccessful() && response.body() != null &&
                response.body().choices != null && !response.body().choices.isEmpty()) {
            aiResponse = response.body().choices.get(0).message.content;
        }

        // Extract JSON from response (Groq decides correctness)
        QuizResult quizResult = extractQuizResult(aiResponse, session, userAnswer);

        if (quizResult != null) {
            quizResult.options = session.currentOptions;
            quizResult.sessionId = session.sessionId;
        }

        // Record conversation
        session.conversationHistory.add(new ChatMessage("user", userAnswer));
        session.conversationHistory.add(new ChatMessage("assistant", aiResponse));

        // Build a user-friendly message using Groq's explanation and correctness
        String displayText;
        if (quizResult != null) {
            String correctness = quizResult.isCorrect ? "✅ Correct!" : "❌ Incorrect.";
            String correctAnswerText = (quizResult.correctAnswer != null && !quizResult.correctAnswer.isEmpty())
                    ? quizResult.correctAnswer
                    : "Not available.";
            String explanationText = (quizResult.explanation != null && !quizResult.explanation.isEmpty())
                    ? quizResult.explanation
                    : "No explanation was provided.";

            displayText = correctness +
                    "\n\nYour answer: " + userAnswer +
                    "\nCorrect answer: " + correctAnswerText +
                    "\n\n" + explanationText;
        } else {
            displayText = aiResponse != null && !aiResponse.isEmpty()
                    ? aiResponse
                    : "I couldn't check that answer right now. Please try again.";
        }

        // Move to next question or finish
        session.currentQuestionIndex++;
        if (session.currentQuestionIndex < session.questionCount) {
            session.state = SessionState.GENERATING_QUESTION;
            return new ReviewerResponse(displayText, quizResult, false);
        } else {
            session.state = SessionState.WAITING_FOR_COMMAND;
            String finalResponse = displayText + "\n\n✅ Completed all " +
                    session.questionCount + " questions!";

            String summary = generateTopicSummary(session);
            if (summary != null && !summary.isEmpty()) {
                finalResponse = finalResponse + "\n\nTopic summary:\n" + summary;
            }

            return new ReviewerResponse(finalResponse, quizResult, false);
        }
    }

    private String generateTopicSummary(ReviewSession session) {
        String subject = session.subject != null ? session.subject : "this topic";
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(
                    "system",
                    "You are a helpful tutor. Provide a brief, student-friendly summary of a topic in 3-4 short sentences."
            ));
            messages.add(new ChatMessage(
                    "user",
                    "Give me a short summary about " + subject + " that reviews what a student should remember."
            ));

            GroqChatRequest request = new GroqChatRequest(messages);
            request.maxTokens = 256;

            retrofit2.Response<GroqChatResponse> response =
                    groqApiService.chatCompletion(request).execute();

            if (response.isSuccessful() && response.body() != null &&
                    response.body().choices != null && !response.body().choices.isEmpty()) {
                return response.body().choices.get(0).message.content;
            }
        } catch (Exception e) {
            android.util.Log.e("ReviewerService", "Error generating topic summary", e);
        }
        return null;
    }

    private QuizResult extractQuizResult(String response, ReviewSession session, String userAnswer) {
        try {
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd);
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

                QuizResult result = new QuizResult();
                result.category = json.has("category") ? json.get("category").getAsString() :
                        (session.subject != null ? session.subject : "General");
                result.question = json.has("question") ? json.get("question").getAsString() :
                        (session.currentQuestion != null ? session.currentQuestion : "");
                result.userAnswer = json.has("user_answer") ? json.get("user_answer").getAsString() : userAnswer;
                result.correctAnswer = json.has("correct_answer") ? json.get("correct_answer").getAsString() : "";
                result.isCorrect = json.has("is_correct") && json.get("is_correct").getAsBoolean();
                result.explanation = json.has("explanation") ? json.get("explanation").getAsString() : "";

                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: create a basic result using Groq-provided question context
        QuizResult result = new QuizResult();
        result.category = session.subject != null ? session.subject : "General";
        result.question = session.currentQuestion != null ? session.currentQuestion : "";
        result.userAnswer = userAnswer;
        result.correctAnswer = "";
        result.isCorrect = false;
        result.explanation = "Could not parse response. Please check manually.";
        return result;
    }

    public ReviewerResponse generateWeakTopicQuestion(ReviewSession session, List<QuizHistory> weakQuestions) throws IOException {
        if (weakQuestions == null || weakQuestions.isEmpty()) {
            return new ReviewerResponse(
                    "I don't have any weak topic data yet. Try taking a regular quiz first!",
                    null, false
            );
        }


        if (session.currentQuestionIndex == 0 || session.sessionId == null || session.sessionId.isEmpty()) {
            session.sessionId = "session_" + System.currentTimeMillis();
        }


        QuizHistory referenceQuestion = weakQuestions.get(0);
        String context = "Generate a new question similar to this one the user got wrong:\n" +
                "Question: \"" + referenceQuestion.question + "\"\n" +
                "Category: \"" + referenceQuestion.category + "\"\n\n" +
                "Create a similar question to help them practice this weak area.";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.add(new ChatMessage("user", context));

        GroqChatRequest request = new GroqChatRequest(messages);
        request.maxTokens = 256;

        retrofit2.Response<GroqChatResponse> response =
                groqApiService.chatCompletion(request).execute();

        String question = "Let me think of a question...";
        if (response.isSuccessful() && response.body() != null &&
                response.body().choices != null && !response.body().choices.isEmpty()) {
            question = response.body().choices.get(0).message.content;
        }

        session.currentQuestion = question;
        session.state = SessionState.WAITING_FOR_ANSWER;

        return new ReviewerResponse(question, null, false);
    }
}

