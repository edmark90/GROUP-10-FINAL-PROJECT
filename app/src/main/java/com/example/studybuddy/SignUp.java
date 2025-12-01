package com.example.studybuddy;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUp extends AppCompatActivity {

    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private MaterialButton buttonSignUp;
    private TextView textLoginLink;
    private FirebaseAuth firebaseAuth;
    private AlertDialog noInternetDialog;
    private SignUp.NetworkChangeReceiver networkReceiver;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textLoginLink = findViewById(R.id.textLoginLink);

        buttonSignUp.setOnClickListener(v -> attemptSignUp());
        textLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });
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

    // Real-time internet monitoring
    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkInternetConnection();
        }
    }

    private void registerNetworkReceiver() {
        networkReceiver = new SignUp.NetworkChangeReceiver();
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


    private void attemptSignUp() {
        String name = editTextName != null && editTextName.getText() != null
                ? editTextName.getText().toString().trim()
                : "";
        String email = editTextEmail != null && editTextEmail.getText() != null
                ? editTextEmail.getText().toString().trim()
                : "";
        String password = editTextPassword != null && editTextPassword.getText() != null
                ? editTextPassword.getText().toString().trim()
                : "";
        String confirmPassword = editTextConfirmPassword != null && editTextConfirmPassword.getText() != null
                ? editTextConfirmPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSignUp.setEnabled(false);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonSignUp.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && !TextUtils.isEmpty(name)) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                        navigateToMain();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Signup failed";
                        Toast.makeText(SignUp.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUp.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}