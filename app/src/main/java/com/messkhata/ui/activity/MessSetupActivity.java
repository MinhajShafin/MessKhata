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
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;
import com.messkhata.utils.PreferenceManager;

/**
 * Activity for creating or joining a mess.
 */
public class MessSetupActivity extends AppCompatActivity {

    private MessDao messDao;
    private UserDao userDao;
    private PreferenceManager prefManager;
    private long userId;

    private MaterialButton btnCreateMess;
    private MaterialButton btnJoinMess;
    private TextInputEditText etMessName, etMessAddress, etJoinCode;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_setup);

        // Initialize DAOs
        messDao = new MessDao(this);
        userDao = new UserDao(this);

        // Get PreferenceManager and userId
        prefManager = PreferenceManager.getInstance(this);
        String userIdStr = prefManager.getUserId();
        
        if (userIdStr == null) {
            // Session invalid - go to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        userId = Long.parseLong(userIdStr);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnCreateMess = findViewById(R.id.btnCreateMess);
        btnJoinMess = findViewById(R.id.btnJoinMess);

        etMessName = findViewById(R.id.etMessName);
        etMessAddress = findViewById(R.id.etMessAddress);
        etJoinCode = findViewById(R.id.etJoinCode);

        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        if (btnCreateMess != null) {
            btnCreateMess.setOnClickListener(v -> {
                String messName = etMessName.getText().toString().trim();

                // Validation
                if (messName.isEmpty()) {
                    etMessName.setError("Mess name is required");
                    return;
                }

                showLoading(true);

                // Create mess in background
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    // Use default values
                    double groceryBudget = 40.00;
                    double cookingCharge = 10.00;

                    long messId = messDao.createMess(messName, groceryBudget, cookingCharge, userId);

                    runOnUiThread(() -> {
                        showLoading(false);

                        if (messId != -1) {
                            // Get updated user info
                            User user = userDao.getUserByIdAsObject((int) userId);
                            
                            if (user != null) {
                                // Update session with mess info
                                prefManager.saveUserSession(
                                    String.valueOf(userId),
                                    String.valueOf(messId),
                                    "ADMIN", // Creator is admin
                                    user.getFullName(),
                                    user.getEmail()
                                );

                                // Get invitation code from messId
                                String invitationCode = messDao.getInvitationCode((int) messId);

                                // Show invitation code
                                Toast.makeText(this,
                                        "Mess created! Invitation Code: " + invitationCode,
                                        Toast.LENGTH_LONG).show();

                                // Navigate to MainActivity
                                Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to create mess", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        }

        if (btnJoinMess != null) {
            btnJoinMess.setOnClickListener(v -> {
                String code = etJoinCode.getText().toString().trim();

                // Validation
                if (code.isEmpty()) {
                    etJoinCode.setError("Invitation code is required");
                    return;
                }

                if (code.length() != 4) {
                    etJoinCode.setError("Code must be 4 digits");
                    return;
                }

                showLoading(true);

                // Join mess in background
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    boolean success = messDao.joinMess(code, userId);

                    runOnUiThread(() -> {
                        showLoading(false);

                        if (success) {
                            // Get updated user info with mess
                            User user = userDao.getUserByIdAsObject((int) userId);
                            
                            if (user != null) {
                                // Update session with mess info
                                prefManager.saveUserSession(
                                    String.valueOf(userId),
                                    String.valueOf(user.getMessId()),
                                    "MEMBER", // Joiners are members
                                    user.getFullName(),
                                    user.getEmail()
                                );

                                Toast.makeText(this, "Joined mess successfully!", Toast.LENGTH_SHORT).show();

                                // Navigate to MainActivity
                                Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Invalid invitation code", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateMess.setEnabled(!show);
        btnJoinMess.setEnabled(!show);
    }
}
