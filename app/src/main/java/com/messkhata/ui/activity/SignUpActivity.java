package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.R;
import com.messkhata.ui.viewmodel.AuthViewModel;

/**
 * Sign Up Activity for new user registration.
 */
public class SignUpActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnSignUp;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupListeners();
        observeAuthResult();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnSignUp.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String name = getText(etName);
        String email = getText(etEmail);
        String phone = getText(etPhone);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Validation
        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        showLoading(true);
        viewModel.signUp(email, password, name, phone);
    }

    private void observeAuthResult() {
        viewModel.getAuthResult().observe(this, result -> {
            showLoading(false);
            if (result.success) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                navigateToMessSetup();
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMessSetup() {
        Intent intent = new Intent(this, MessSetupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!show);
    }
}
