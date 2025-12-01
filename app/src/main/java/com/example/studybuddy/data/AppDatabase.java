package com.example.studybuddy.data;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {QuizHistory.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    public abstract QuizHistoryDao quizHistoryDao();
    
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "studybuddy_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build();
                        Log.d(TAG, "Database initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing database", e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }
}

