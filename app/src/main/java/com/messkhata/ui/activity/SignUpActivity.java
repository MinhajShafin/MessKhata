package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.messkhata.R;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.sync.FirebaseAuthHelper;

/**
 * Sign Up Activity for new user registration.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnSignUp;
    private Button btnLogin;
    private View progressBar;

    private UserDao userDao;
    private FirebaseAuthHelper firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize DAO
        userDao = new UserDao(this);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuthHelper.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
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

        if (phone.isEmpty()) {
            etPhone.setError("Phone is required");
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

        // First, create Firebase account
        firebaseAuth.signUpWithEmail(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                // Firebase account created, now create local account
                createLocalAccount(name, email, phone, password, firebaseUser);
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);

                    // Check if email already exists in Firebase
                    if (error.contains("email address is already in use")) {
                        Toast.makeText(SignUpActivity.this,
                                "This email is already registered. Please login instead.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Registration failed: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Create local database account after Firebase signup
     */
    private void createLocalAccount(String name, String email, String phone,
            String password, FirebaseUser firebaseUser) {
        // Register user in background thread
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            boolean success = userDao.registerUser(name, email, phone, password);

            // Update UI on main thread
            runOnUiThread(() -> {
                showLoading(false);
                if (success) {
                    // Update Firebase display name
                    firebaseAuth.updateDisplayName(name, new FirebaseAuthHelper.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            // Name updated
                        }

                        @Override
                        public void onFailure(String error) {
                            // Non-critical, continue anyway
                        }
                    });

                    Toast.makeText(SignUpActivity.this,
                            "Registration successful!",
                            Toast.LENGTH_SHORT).show();

                    // Go to login screen
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    // Local registration failed, delete Firebase account
                    firebaseAuth.deleteAccount(new FirebaseAuthHelper.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                        }

                        @Override
                        public void onFailure(String error) {
                        }
                    });

                    Toast.makeText(SignUpActivity.this,
                            "Registration failed. Email or phone already exists.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!show);
    }
}