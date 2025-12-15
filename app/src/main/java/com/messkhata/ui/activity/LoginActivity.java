package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.ui.viewmodel.AuthViewModel;

/**
 * Login Activity for user authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnSignUp;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Check if already logged in
        if (viewModel.isLoggedIn()) {
            navigateToNextScreen();
            return;
        }

        initViews();
        setupListeners();
        observeAuthResult();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSignUp.setOnClickListener(v -> navigateToSignUp());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }

        showLoading(true);
        viewModel.signIn(email, password);
    }

    private void observeAuthResult() {
        viewModel.getAuthResult().observe(this, result -> {
            showLoading(false);
            if (result.success) {
                navigateToNextScreen();
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToNextScreen() {
        Intent intent;
        if (viewModel.hasJoinedMess()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, MessSetupActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnSignUp.setEnabled(!show);
    }
}
