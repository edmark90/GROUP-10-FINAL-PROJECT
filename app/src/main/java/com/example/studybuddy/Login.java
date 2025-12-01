package com.example.studybuddy;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

public class Login extends AppCompatActivity {

	private TextInputEditText editTextEmail;
	private TextInputEditText editTextPassword;
	private MaterialButton buttonLogin;
	private TextView textSignUpLink;
	private FirebaseAuth firebaseAuth;
	private AlertDialog noInternetDialog;
	private NetworkChangeReceiver networkReceiver;

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
		setContentView(R.layout.activity_login);



		firebaseAuth = FirebaseAuth.getInstance();

		editTextEmail = findViewById(R.id.editTextEmail);
		editTextPassword = findViewById(R.id.editTextPassword);
		buttonLogin = findViewById(R.id.buttonLogin);
		textSignUpLink = findViewById(R.id.textSignUpLink);

		buttonLogin.setOnClickListener(v -> attemptLogin());
		textSignUpLink.setOnClickListener(v -> {
			Intent intent = new Intent(Login.this, SignUp.class);
			startActivity(intent);
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// I-register para mamonitor ang internet changes
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
		networkReceiver = new NetworkChangeReceiver();
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

		// Black background
		noInternetDialog.getWindow().setBackgroundDrawableResource(android.R.color.darker_gray);

		okButton.setOnClickListener(v -> {
			noInternetDialog.dismiss();
			checkInternetConnection();
		});
	}



	private void attemptLogin() {
		String email = editTextEmail != null && editTextEmail.getText() != null
				? editTextEmail.getText().toString().trim()
				: "";
		String password = editTextPassword != null && editTextPassword.getText() != null
				? editTextPassword.getText().toString().trim()
				: "";

		if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
			Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
			return;
		}

		buttonLogin.setEnabled(false);
		firebaseAuth.signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(this, task -> {
					buttonLogin.setEnabled(true);
					if (task.isSuccessful()) {
						navigateToMain();
					} else {
						String message = task.getException() != null
								? task.getException().getMessage()
								: "Authentication failed";
						Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
					}
				});
	}

	private void navigateToMain() {
		Intent intent = new Intent(Login.this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}
}