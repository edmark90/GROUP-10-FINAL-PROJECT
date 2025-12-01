package com.example.studybuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studybuddy.api.ChatMessage;
import com.example.studybuddy.databinding.ItemMessageBinding;
import com.example.studybuddy.databinding.ItemMessageWithOptionsBinding;
import com.example.studybuddy.R;

public class MessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    private OnOptionSelectedListener optionListener;
    
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_WITH_OPTIONS = 1;
    
    public interface OnOptionSelectedListener {
        void onOptionSelected(String option);
    }
    
    public MessageAdapter() {
        super(new MessageDiffCallback());
    }
    
    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.optionListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.hasOptions() && "assistant".equals(message.role)) {
            return TYPE_WITH_OPTIONS;
        }
        return TYPE_NORMAL;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_WITH_OPTIONS) {
            ItemMessageWithOptionsBinding binding = ItemMessageWithOptionsBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            );
            return new MessageWithOptionsViewHolder(binding);
        } else {
            ItemMessageBinding binding = ItemMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            );
            return new MessageViewHolder(binding);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (holder instanceof MessageWithOptionsViewHolder) {
            ((MessageWithOptionsViewHolder) holder).bind(message, optionListener);
        } else {
            ((MessageViewHolder) holder).bind(message);
        }
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private ItemMessageBinding binding;
        
        MessageViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(ChatMessage message) {
            binding.textMessage.setText(message.content);
            
            android.content.Context context = binding.getRoot().getContext();
            android.widget.LinearLayout root = (android.widget.LinearLayout) binding.getRoot();
            
            // Style based on role
            if ("user".equals(message.role)) {
                root.setGravity(android.view.Gravity.END);
                binding.imageAvatar.setVisibility(View.GONE);
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.white)
                );
                binding.textMessage.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary_black)
                );
            } else {
                root.setGravity(android.view.Gravity.START);
                binding.imageAvatar.setVisibility(View.VISIBLE);
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_black)
                );
                binding.textMessage.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary_white)
                );
            }
        }
    }
    
    static class MessageWithOptionsViewHolder extends RecyclerView.ViewHolder {
        private ItemMessageWithOptionsBinding binding;
        
        MessageWithOptionsViewHolder(ItemMessageWithOptionsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(ChatMessage message, OnOptionSelectedListener listener) {
            // Extract question text (before options)
            String fullText = message.content;
            String questionText = fullText;
            if (fullText.contains("\n\n")) {
                questionText = fullText.substring(0, fullText.indexOf("\n\n"));
            }
            binding.textMessage.setText(questionText);
            
            // Show options
            String[] options = message.options;
            if (options != null && options.length >= 4) {
                // Ensure buttons are enabled when (re)binding a question with options
                binding.buttonOptionA.setEnabled(true);
                binding.buttonOptionB.setEnabled(true);
                binding.buttonOptionC.setEnabled(true);
                binding.buttonOptionD.setEnabled(true);

                binding.optionsContainer.setVisibility(View.VISIBLE);
                binding.buttonOptionA.setText("A) " + options[0]);
                binding.buttonOptionB.setText("B) " + options[1]);
                binding.buttonOptionC.setText("C) " + options[2]);
                binding.buttonOptionD.setText("D) " + options[3]);
                
                // Set click listeners
                View.OnClickListener optionClick = v -> {
                    if (listener != null) {
                        String selected = "";
                        if (v == binding.buttonOptionA) selected = options[0];
                        else if (v == binding.buttonOptionB) selected = options[1];
                        else if (v == binding.buttonOptionC) selected = options[2];
                        else if (v == binding.buttonOptionD) selected = options[3];
                        listener.onOptionSelected(selected);
                    }

                    // Disable all option buttons after one selection
                    binding.buttonOptionA.setEnabled(false);
                    binding.buttonOptionB.setEnabled(false);
                    binding.buttonOptionC.setEnabled(false);
                    binding.buttonOptionD.setEnabled(false);
                };
                
                binding.buttonOptionA.setOnClickListener(optionClick);
                binding.buttonOptionB.setOnClickListener(optionClick);
                binding.buttonOptionC.setOnClickListener(optionClick);
                binding.buttonOptionD.setOnClickListener(optionClick);
            } else {
                // Hide options if not available
                binding.optionsContainer.setVisibility(View.GONE);
            }
            
            // Style for assistant messages
            android.content.Context context = binding.getRoot().getContext();
            binding.cardMessage.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.surface_black)
            );
            binding.textMessage.setTextColor(
                ContextCompat.getColor(context, R.color.text_primary_white)
            );
        }
    }
    
    static class MessageDiffCallback extends DiffUtil.ItemCallback<ChatMessage> {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem == newItem;
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.content.equals(newItem.content) && 
                   oldItem.role.equals(newItem.role);
        }
    }
}
