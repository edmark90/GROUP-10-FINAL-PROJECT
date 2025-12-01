package com.example.studybuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private QuizSessionAdapter adapter;
    private AppDatabase database;
    private QuizHistoryDao dao;
    private ExecutorService executorService;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );
        setContentView(com.example.studybuddy.R.layout.activity_history);

        database = AppDatabase.getDatabase(this);
        dao = database.quizHistoryDao();
        executorService = Executors.newSingleThreadExecutor();

        toolbar = findViewById(com.example.studybuddy.R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quiz History");
        }

        recyclerView = findViewById(com.example.studybuddy.R.id.recyclerViewHistory);
        TextView emptyText = findViewById(com.example.studybuddy.R.id.textEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizSessionAdapter(new ArrayList<>(), new QuizSessionAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(String sessionId) {
                Intent intent = new Intent(HistoryActivity.this, QuizSessionDetailActivity.class);
                intent.putExtra("sessionId", sessionId);
                startActivity(intent);
            }

            @Override
            public void onSessionDeleteClick(String sessionId) {
                // Show confirmation alert before deleting
                showDeleteConfirmation(sessionId);
            }
        });
        recyclerView.setAdapter(adapter);

        // Show empty message if no history
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        loadHistory();
    }

    private void showDeleteConfirmation(String sessionId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Quiz Session")
                .setMessage("Are you sure you want to delete this quiz session?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Proceed with deletion
                    deleteSession(sessionId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSession(String sessionId) {
        executorService.execute(() -> {
            try {
                dao.deleteBySessionId(sessionId);
                runOnUiThread(() -> loadHistory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadHistory() {
        executorService.execute(() -> {
            try {
                List<String> sessionIds = dao.getAllSessionIds();
                List<QuizSession> sessions = new ArrayList<>();

                for (String sessionId : sessionIds) {
                    List<QuizHistory> questions = dao.getQuestionsBySession(sessionId);
                    if (questions != null && !questions.isEmpty()) {
                        QuizSession session = new QuizSession();
                        session.sessionId = sessionId;
                        session.questions = questions;
                        session.category = questions.get(0).category != null ? questions.get(0).category : "General";
                        session.timestamp = questions.get(0).timestamp;
                        session.totalQuestions = questions.size();
                        int correctCount = 0;
                        for (QuizHistory q : questions) {
                            if (q.isCorrect) correctCount++;
                        }
                        session.correctCount = correctCount;
                        sessions.add(session);
                    }
                }

                runOnUiThread(() -> {
                    adapter.updateSessions(sessions);
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

    static class QuizSession {
        String sessionId;
        String category;
        long timestamp;
        int totalQuestions;
        int correctCount;
        List<QuizHistory> questions;
    }
}