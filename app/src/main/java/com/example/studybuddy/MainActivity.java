package com.example.studybuddy;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studybuddy.databinding.ActivityReviewerBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ActivityReviewerBinding binding;
    private ReviewerViewModel viewModel;
    private MessageAdapter messageAdapter;
    private AlertDialog noInternetDialog;
    private MainActivity.NetworkChangeReceiver networkReceiver;
    
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
        binding = ActivityReviewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("StudyBuddy");
        }
        
        viewModel = new ViewModelProvider(this).get(ReviewerViewModel.class);
        
        setupRecyclerView();
        setupInput();

        observeViewModel();
        
        // Start session on launch
        if (savedInstanceState == null) {
            viewModel.startSession();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" Iiwan mo na si StudyBuddy? 🥲")
                .setMessage("Sana hindi ka magsisi!")
                .setPositiveButton("YES", (dialog, which) -> performLogout())
                .setNegativeButton("NO", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set black background
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);

        // Set ALL text to white
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);

        // Set title text color to white
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId > 0) {
            TextView titleView = dialog.findViewById(titleId);
            if (titleView != null) {
                titleView.setTextColor(Color.WHITE);
            }
        }

        // Set message text color to white
        int messageId = getResources().getIdentifier("message", "id", "android");
        if (messageId > 0) {
            TextView messageView = dialog.findViewById(messageId);
            if (messageView != null) {
                messageView.setTextColor(Color.WHITE);
            }
        }
    }


    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerNetworkReceiver();
        checkInternetConnection();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // I-unregister at isara dialog
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }
    }


    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkInternetConnection();
        }
    }

    private void registerNetworkReceiver() {
        networkReceiver = new MainActivity.NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    private void checkInternetConnection() {
        boolean hasInternet = NetworkUtils.isNetworkAvailable(this);

        runOnUiThread(() -> {
            if (hasInternet) {
                // May internet - isara dialog
                if (noInternetDialog != null && noInternetDialog.isShowing()) {
                    noInternetDialog.dismiss();
                }

            } else {

                showNoInternetDialog();
            }
        });
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_internet, null);
        Button okButton = dialogView.findViewById(R.id.ok_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(false);

        noInternetDialog = builder.create();
        noInternetDialog.show();


        noInternetDialog.getWindow().setBackgroundDrawableResource(android.R.color.darker_gray);

        okButton.setOnClickListener(v -> {
            noInternetDialog.dismiss();
            checkInternetConnection();
        });
    }





    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_logout) {
            showLogoutConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter();
        messageAdapter.setOnOptionSelectedListener(option -> {
            // When user selects an option, send it as a message
            viewModel.sendMessage(option);
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        binding.recyclerViewMessages.setLayoutManager(layoutManager);
        binding.recyclerViewMessages.setAdapter(messageAdapter);
        
        // Auto-scroll to bottom when new messages arrive
        messageAdapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int messageCount = messageAdapter.getItemCount();
                if (messageCount > 0) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messageCount - 1);
                }
            }
        });
    }
    
    private void setupInput() {
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        
        binding.editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    private void sendMessage() {
        String message = binding.editTextMessage.getText() != null ? 
                        binding.editTextMessage.getText().toString().trim() : "";
        if (!message.isEmpty()) {
            viewModel.sendMessage(message);
            binding.editTextMessage.setText("");
        }
    }
    
    private void observeViewModel() {
        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.submitList(messages);
            // Scroll to bottom after list is updated
            if (messages != null && !messages.isEmpty()) {
                binding.recyclerViewMessages.post(() -> {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size() - 1);
                });
            }
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressIndicator.setVisibility(isLoading ? 
                android.view.View.VISIBLE : android.view.View.GONE);
            binding.buttonSend.setEnabled(!isLoading);
            binding.editTextMessage.setEnabled(!isLoading);
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}

