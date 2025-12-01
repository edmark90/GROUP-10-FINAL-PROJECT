package com.example.studybuddy.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuizHistoryDao {
    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    List<QuizHistory> getAllHistory();
    
    @Insert
    void insert(QuizHistory quizHistory);
    
    @Query("SELECT DISTINCT category FROM quiz_history")
    List<String> getAllCategories();
    
    @Query("SELECT category, " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN isCorrect = 0 THEN 1 ELSE 0 END) as incorrect " +
           "FROM quiz_history " +
           "GROUP BY category")
    List<CategoryPerformance> getCategoryPerformance();
    
    @Query("SELECT * FROM quiz_history " +
           "WHERE category IN (:categories) " +
           "AND isCorrect = 0 " +
           "ORDER BY lastReviewed ASC, reviewCount ASC " +
           "LIMIT :limit")
    List<QuizHistory> getWeakQuestions(List<String> categories, int limit);
    
    @Query("SELECT * FROM quiz_history " +
           "WHERE category IN (:categories) " +
           "ORDER BY lastReviewed ASC, reviewCount ASC " +
           "LIMIT :limit")
    List<QuizHistory> getQuestionsByCategories(List<String> categories, int limit);
    
    @Update
    void update(QuizHistory quizHistory);
    
    @Query("SELECT DISTINCT sessionId FROM quiz_history WHERE sessionId IS NOT NULL ORDER BY timestamp DESC")
    List<String> getAllSessionIds();
    
    @Query("SELECT * FROM quiz_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<QuizHistory> getQuestionsBySession(String sessionId);

    @Query("DELETE FROM quiz_history WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM quiz_history WHERE sessionId = :sessionId")
    void deleteBySessionId(String sessionId);

    @Query("DELETE FROM quiz_history")
    void deleteAll();
}
