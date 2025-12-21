package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;
import com.messkhata.utils.PreferenceManager;

/**
 * Login Activity for user authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnSignUp;
    private View progressBar;

    private UserDao userDao;
    private PreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize DAO
        userDao = new UserDao(this);

        // Initialize PreferenceManager
        prefManager = PreferenceManager.getInstance(this);

        initViews();
        setupListeners();

        // Pre-fill email if coming from SignUp
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            etEmail.setText(email);
        }
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

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void attemptLogin() {
        String email = getText(etEmail);
        String password = getText(etPassword);

        // Validation
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }

        showLoading(true);

        // Login in background thread
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            long userId = userDao.loginUser(email, password);

            // Update UI on main thread
            runOnUiThread(() -> {
                showLoading(false);

                if (userId != -1) {
                    // Get full user details
                    User user = userDao.getUserByIdAsObject((int) userId);
                    
                    if (user != null) {
                        // Save user session with PreferenceManager
                        prefManager.saveUserSession(
                            String.valueOf(userId),
                            String.valueOf(user.getMessId()),
                            user.getRole(),
                            user.getFullName(),
                            user.getEmail()
                        );

                        Toast.makeText(LoginActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if (user.getMessId() > 0) {
                            // User already in mess - go to MainActivity
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                        } else {
                            // User needs to create/join mess
                            intent = new Intent(LoginActivity.this, MessSetupActivity.class);
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error loading user data",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Invalid email or password",
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
        btnLogin.setEnabled(!show);
        btnSignUp.setEnabled(!show);
    }
}