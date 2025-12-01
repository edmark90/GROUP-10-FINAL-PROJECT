package com.example.studybuddy;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studybuddy.data.AppDatabase;
import com.example.studybuddy.data.QuizHistory;
import com.example.studybuddy.data.QuizHistoryDao;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizSessionDetailActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase database;
    private QuizHistoryDao dao;
    private ExecutorService executorService;
    private String sessionId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.studybuddy.R.layout.activity_quiz_session_detail);
        
        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            finish();
            return;
        }
        
        database = AppDatabase.getDatabase(this);
        dao = database.quizHistoryDao();
        executorService = Executors.newSingleThreadExecutor();
        
        MaterialToolbar toolbar = findViewById(com.example.studybuddy.R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quiz Session");
        }
        
        recyclerView = findViewById(com.example.studybuddy.R.id.recyclerViewQuestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(new ArrayList<>(), id -> {
            executorService.execute(() -> {
                try {
                    dao.deleteById(id);
                    List<QuizHistory> questions = dao.getQuestionsBySession(sessionId);
                    runOnUiThread(() -> {
                        adapter.updateHistory(questions != null ? questions : new ArrayList<>());
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        recyclerView.setAdapter(adapter);
        
        loadSessionQuestions();
    }
    
    private void loadSessionQuestions() {
        executorService.execute(() -> {
            try {
                List<QuizHistory> questions = dao.getQuestionsBySession(sessionId);
                runOnUiThread(() -> {
                    adapter.updateHistory(questions != null ? questions : new ArrayList<>());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

