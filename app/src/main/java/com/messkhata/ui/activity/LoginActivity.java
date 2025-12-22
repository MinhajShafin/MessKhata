package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;
import com.messkhata.data.sync.FirebaseAuthHelper;
import com.messkhata.data.sync.SyncManager;
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
    private FirebaseAuthHelper firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize DAO
        userDao = new UserDao(this);

        // Initialize PreferenceManager
        prefManager = PreferenceManager.getInstance(this);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuthHelper.getInstance();

        initViews();
        setupListeners();

        // Check if already signed in with Firebase
        if (firebaseAuth.isSignedIn()) {
            checkExistingSession();
        }

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

    /**
     * Check if user has existing session and auto-login
     */
    private void checkExistingSession() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            if (email != null) {
                showLoading(true);

                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    // Find user in local database by email
                    long userId = findUserIdByEmail(email);

                    runOnUiThread(() -> {
                        showLoading(false);
                        if (userId != -1) {
                            completeLogin(userId);
                        }
                    });
                });
            }
        }
    }

    /**
     * Find user ID by email in local database
     */
    private long findUserIdByEmail(String email) {
        android.database.Cursor cursor = null;
        try {
            cursor = userDao.getUserByEmail(email);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow("userId"));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return -1;
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

        // First, authenticate with Firebase
        firebaseAuth.signInWithEmail(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                // Firebase auth successful, now verify with local database
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    long userId = userDao.loginUser(email, password);

                    runOnUiThread(() -> {
                        if (userId != -1) {
                            completeLogin(userId);
                        } else {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Local account not found. Please sign up first.",
                                    Toast.LENGTH_SHORT).show();
                            // Sign out from Firebase since local account doesn't exist
                            firebaseAuth.signOut();
                        }
                    });
                });
            }

            @Override
            public void onFailure(String error) {
                // Firebase auth failed, try local-only login as fallback
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    long userId = userDao.loginUser(email, password);

                    runOnUiThread(() -> {
                        showLoading(false);

                        if (userId != -1) {
                            // Local login worked, try to create Firebase account
                            tryCreateFirebaseAccount(email, password, userId);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Invalid email or password",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }

    /**
     * Try to create Firebase account for existing local user
     */
    private void tryCreateFirebaseAccount(String email, String password, long userId) {
        firebaseAuth.signUpWithEmail(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Account created, complete login
                completeLogin(userId);
            }

            @Override
            public void onFailure(String error) {
                // Could not create Firebase account, but local login worked
                // Continue with local-only mode
                completeLogin(userId);
            }
        });
    }

    /**
     * Complete the login process after authentication
     */
    private void completeLogin(long userId) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User user = userDao.getUserByIdAsObject((int) userId);

            runOnUiThread(() -> {
                showLoading(false);

                if (user != null) {
                    // Save user session with PreferenceManager
                    prefManager.saveUserSession(
                            String.valueOf(userId),
                            String.valueOf(user.getMessId()),
                            user.getRole(),
                            user.getFullName(),
                            user.getEmail());

                    Toast.makeText(LoginActivity.this,
                            "Login successful!",
                            Toast.LENGTH_SHORT).show();

                    // Trigger sync if user has a mess
                    if (user.getMessId() > 0) {
                        SyncManager.getInstance(LoginActivity.this)
                                .performFullSync(user.getMessId());
                    }

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