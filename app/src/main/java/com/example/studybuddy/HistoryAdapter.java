package com.example.studybuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studybuddy.data.QuizHistory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<QuizHistory> historyList;
    private SimpleDateFormat dateFormat;
    private OnHistoryDeleteListener deleteListener;
    
    public HistoryAdapter(List<QuizHistory> historyList, OnHistoryDeleteListener deleteListener) {
        this.historyList = historyList != null ? historyList : new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.deleteListener = deleteListener;
    }
    
    public void updateHistory(List<QuizHistory> newHistory) {
        this.historyList = newHistory != null ? newHistory : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(com.example.studybuddy.R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizHistory item = historyList.get(position);
        
        holder.categoryText.setText(item.category != null ? item.category : "General");
        holder.questionText.setText(item.question != null ? item.question : "");
        holder.userAnswerText.setText("Your answer: " + (item.userAnswer != null ? item.userAnswer : ""));
        holder.correctAnswerText.setText("Correct: " + (item.correctAnswer != null ? item.correctAnswer : ""));
        holder.explanationText.setText(item.explanation != null ? item.explanation : "");
        
        // Show options if available
        if (item.options != null && !item.options.isEmpty()) {
            try {
                String[] options = new com.google.gson.Gson().fromJson(item.options, String[].class);
                if (options != null && options.length > 0) {
                    StringBuilder optionsText = new StringBuilder("Options: ");
                    for (int i = 0; i < options.length; i++) {
                        optionsText.append((char)('A' + i)).append(") ").append(options[i]);
                        if (i < options.length - 1) optionsText.append(", ");
                    }
                    holder.optionsText.setText(optionsText.toString());
                    holder.optionsText.setVisibility(android.view.View.VISIBLE);
                } else {
                    holder.optionsText.setVisibility(android.view.View.GONE);
                }
            } catch (Exception e) {
                holder.optionsText.setVisibility(android.view.View.GONE);
            }
        } else {
            holder.optionsText.setVisibility(android.view.View.GONE);
        }
        
        if (item.timestamp > 0) {
            holder.dateText.setText(dateFormat.format(new Date(item.timestamp)));
        } else {
            holder.dateText.setText("");
        }
        
        // Set color based on correctness
        int bgColor = item.isCorrect ? 
            com.example.studybuddy.R.color.surface_white : 
            com.example.studybuddy.R.color.black_10;
        holder.itemView.setBackgroundColor(holder.itemView.getContext().getColor(bgColor));
        
        // Show result indicator
        String result = item.isCorrect ? "✓ Correct" : "✗ Incorrect";
        holder.resultText.setText(result);
        holder.resultText.setTextColor(holder.itemView.getContext().getColor(
            item.isCorrect ? com.example.studybuddy.R.color.black : com.example.studybuddy.R.color.gray_700
        ));

        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onHistoryDelete(item.id);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText;
        TextView questionText;
        TextView userAnswerText;
        TextView correctAnswerText;
        TextView explanationText;
        TextView dateText;
        TextView resultText;
        TextView optionsText;
        android.widget.ImageButton deleteButton;
        
        ViewHolder(View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(com.example.studybuddy.R.id.textCategory);
            questionText = itemView.findViewById(com.example.studybuddy.R.id.textQuestion);
            userAnswerText = itemView.findViewById(com.example.studybuddy.R.id.textUserAnswer);
            correctAnswerText = itemView.findViewById(com.example.studybuddy.R.id.textCorrectAnswer);
            explanationText = itemView.findViewById(com.example.studybuddy.R.id.textExplanation);
            dateText = itemView.findViewById(com.example.studybuddy.R.id.textDate);
            resultText = itemView.findViewById(com.example.studybuddy.R.id.textResult);
            optionsText = itemView.findViewById(com.example.studybuddy.R.id.textOptions);
            deleteButton = itemView.findViewById(com.example.studybuddy.R.id.buttonDeleteEntry);
        }
    }

    public interface OnHistoryDeleteListener {
        void onHistoryDelete(long id);
    }
}

