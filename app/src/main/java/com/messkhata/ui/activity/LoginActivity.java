package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.data.dao.MessDao;
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

    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnSignUp;
    private View progressBar;

    private UserDao userDao;
    private MessDao messDao;
    private PreferenceManager prefManager;
    private FirebaseAuthHelper firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize DAOs
        userDao = new UserDao(this);
        messDao = new MessDao(this);

        // Initialize PreferenceManager
        prefManager = PreferenceManager.getInstance(this);

        // Initialize Firebase
        firebaseAuth = FirebaseAuthHelper.getInstance();
        firestore = FirebaseFirestore.getInstance();

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
                // Firebase auth successful, now check local database
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    long userId = userDao.loginUser(email, password);

                    runOnUiThread(() -> {
                        if (userId != -1) {
                            // User exists locally
                            completeLogin(userId);
                        } else {
                            // User doesn't exist locally - fetch from Firestore
                            fetchUserFromFirestore(email, password);
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
     * Fetch user data from Firestore and create local account
     */
    private void fetchUserFromFirestore(String email, String password) {
        Log.d(TAG, "Fetching user from Firestore: " + email);

        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User found in Firestore
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        createLocalUserFromFirestore(userDoc, password);
                    } else {
                        // User not in Firestore either - create new local account
                        Log.d(TAG, "User not found in Firestore, creating local account");
                        createNewLocalUser(email, password);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user from Firestore", e);
                    // Try creating local account as fallback
                    createNewLocalUser(email, password);
                });
    }

    /**
     * Create local user from Firestore data
     */
    private void createLocalUserFromFirestore(DocumentSnapshot userDoc, String password) {
        String email = userDoc.getString("email");
        String fullName = userDoc.getString("fullName");
        String phoneNumber = userDoc.getString("phoneNumber");
        String role = userDoc.getString("role");
        Long messIdLong = userDoc.getLong("messId");
        String firebaseMessId = userDoc.getString("firebaseMessId");

        if (fullName == null)
            fullName = "User";
        if (phoneNumber == null)
            phoneNumber = "";
        if (role == null)
            role = "member";
        int messId = messIdLong != null ? messIdLong.intValue() : -1;

        final String finalName = fullName;
        final String finalPhone = phoneNumber;
        final String finalRole = role;
        final int finalMessId = messId;

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            // Register user locally
            boolean success = userDao.registerUser(finalName, email, finalPhone, password);

            if (success) {
                // Get the newly created user ID
                long userId = userDao.loginUser(email, password);

                if (userId != -1 && finalMessId > 0) {
                    // If user had a mess, we need to sync that too
                    syncUserMessFromFirestore(userId, firebaseMessId, finalRole);
                } else {
                    runOnUiThread(() -> {
                        if (userId != -1) {
                            completeLogin(userId);
                        } else {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Failed to create local account",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Failed to create local account",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Sync user's mess from Firestore
     */
    private void syncUserMessFromFirestore(long userId, String firebaseMessId, String role) {
        if (firebaseMessId == null || firebaseMessId.isEmpty()) {
            runOnUiThread(() -> completeLogin(userId));
            return;
        }

        firestore.collection("messes")
                .document(firebaseMessId)
                .get()
                .addOnSuccessListener(messDoc -> {
                    if (messDoc.exists()) {
                        String messName = messDoc.getString("messName");
                        String invitationCode = messDoc.getString("invitationCode");
                        Double groceryBudget = messDoc.getDouble("groceryBudgetPerMeal");
                        Double cookingCharge = messDoc.getDouble("cookingChargePerMeal");
                        Long createdDate = messDoc.getLong("createdDate");

                        if (groceryBudget == null)
                            groceryBudget = 40.0;
                        if (cookingCharge == null)
                            cookingCharge = 10.0;
                        if (createdDate == null)
                            createdDate = System.currentTimeMillis() / 1000;

                        final double finalGrocery = groceryBudget;
                        final double finalCooking = cookingCharge;
                        final long finalCreated = createdDate;
                        final String finalCode = invitationCode;

                        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                            // Check if mess exists locally
                            int existingMessId = messDao.getMessIdByFirebaseId(firebaseMessId);

                            long localMessId;
                            if (existingMessId > 0) {
                                localMessId = existingMessId;
                            } else {
                                // Create mess locally
                                localMessId = messDao.createMessWithDetails(
                                        messName, finalGrocery, finalCooking, finalCreated);
                                if (localMessId > 0) {
                                    messDao.saveFirebaseMessId((int) localMessId, firebaseMessId, finalCode);
                                }
                            }

                            if (localMessId > 0) {
                                // Update user's mess
                                userDao.updateUserMessId(userId, (int) localMessId);
                                userDao.updateUserRole(userId, role);
                            }

                            runOnUiThread(() -> completeLogin(userId));
                        });
                    } else {
                        runOnUiThread(() -> completeLogin(userId));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching mess from Firestore", e);
                    runOnUiThread(() -> completeLogin(userId));
                });
    }

    /**
     * Create a new local user when Firebase has auth but no Firestore data
     */
    private void createNewLocalUser(String email, String password) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String displayName = firebaseUser != null && firebaseUser.getDisplayName() != null
                ? firebaseUser.getDisplayName()
                : "User";

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            boolean success = userDao.registerUser(displayName, email, "", password);

            if (success) {
                long userId = userDao.loginUser(email, password);
                runOnUiThread(() -> {
                    if (userId != -1) {
                        completeLogin(userId);
                    } else {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Failed to create local account",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Failed to create local account",
                            Toast.LENGTH_SHORT).show();
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