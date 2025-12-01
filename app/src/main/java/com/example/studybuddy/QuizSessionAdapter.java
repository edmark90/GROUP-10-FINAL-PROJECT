package com.example.studybuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizSessionAdapter extends RecyclerView.Adapter<QuizSessionAdapter.ViewHolder> {
    private List<HistoryActivity.QuizSession> sessions;
    private OnSessionClickListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnSessionClickListener {
        void onSessionClick(String sessionId);
        void onSessionDeleteClick(String sessionId);
    }
    
    public QuizSessionAdapter(List<HistoryActivity.QuizSession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }
    
    public void updateSessions(List<HistoryActivity.QuizSession> newSessions) {
        this.sessions = newSessions != null ? newSessions : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(com.example.studybuddy.R.layout.item_quiz_session, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryActivity.QuizSession session = sessions.get(position);
        
        holder.categoryText.setText(session.category != null ? session.category : "General");
        holder.scoreText.setText(session.correctCount + "/" + session.totalQuestions);
        holder.questionsText.setText(session.totalQuestions + " questions");
        
        if (session.timestamp > 0) {
            holder.dateText.setText(dateFormat.format(new Date(session.timestamp)));
        } else {
            holder.dateText.setText("");
        }
        
        // Calculate percentage
        double percentage = session.totalQuestions > 0 ? 
            (double) session.correctCount / session.totalQuestions * 100 : 0;
        holder.percentageText.setText(String.format(Locale.getDefault(), "%.0f%%", percentage));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionClick(session.sessionId);
            }
        });

        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionDeleteClick(session.sessionId);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return sessions.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText;
        TextView scoreText;
        TextView questionsText;
        TextView dateText;
        TextView percentageText;
        MaterialCardView cardView;
        android.widget.ImageButton deleteButton;
        
        ViewHolder(View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(com.example.studybuddy.R.id.textCategory);
            scoreText = itemView.findViewById(com.example.studybuddy.R.id.textScore);
            questionsText = itemView.findViewById(com.example.studybuddy.R.id.textQuestions);
            dateText = itemView.findViewById(com.example.studybuddy.R.id.textDate);
            percentageText = itemView.findViewById(com.example.studybuddy.R.id.textPercentage);
            cardView = (MaterialCardView) itemView;
            deleteButton = itemView.findViewById(com.example.studybuddy.R.id.buttonDeleteSession);
        }
    }
}

