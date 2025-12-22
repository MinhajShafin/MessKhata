package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;
import com.messkhata.data.sync.SyncManager;
import com.messkhata.utils.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Activity for creating or joining a mess.
 */
public class MessSetupActivity extends AppCompatActivity {

    private static final String TAG = "MessSetupActivity";

    private MessDao messDao;
    private UserDao userDao;
    private PreferenceManager prefManager;
    private FirebaseFirestore firestore;
    private SyncManager syncManager;
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

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize SyncManager
        syncManager = SyncManager.getInstance(this);

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
            btnCreateMess.setOnClickListener(v -> createMess());
        }

        if (btnJoinMess != null) {
            btnJoinMess.setOnClickListener(v -> joinMess());
        }
    }

    /**
     * Generate a unique 6-digit invitation code
     */
    private String generateInvitationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code: 100000-999999
        return String.valueOf(code);
    }

    /**
     * Create mess in Firebase first, then sync to local
     */
    private void createMess() {
        String messName = etMessName.getText().toString().trim();

        // Validation
        if (messName.isEmpty()) {
            etMessName.setError("Mess name is required");
            return;
        }

        showLoading(true);

        // Generate unique invitation code
        String invitationCode = generateInvitationCode();

        // Check if code already exists in Firebase
        firestore.collection("messes")
                .whereEqualTo("invitationCode", invitationCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        // Code is unique, proceed with creation
                        createMessInFirebase(messName, invitationCode);
                    } else {
                        // Code exists, generate a new one and retry
                        String newCode = generateInvitationCode();
                        createMessInFirebase(messName, newCode);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking code uniqueness", e);
                    // Proceed anyway, Firebase will handle duplicates
                    createMessInFirebase(messName, invitationCode);
                });
    }

    /**
     * Create mess document in Firebase
     */
    private void createMessInFirebase(String messName, String invitationCode) {
        double groceryBudget = 40.00;
        double cookingCharge = 10.00;
        long createdDate = System.currentTimeMillis() / 1000;

        // Create mess data for Firebase
        Map<String, Object> messData = new HashMap<>();
        messData.put("messName", messName);
        messData.put("invitationCode", invitationCode);
        messData.put("groceryBudgetPerMeal", groceryBudget);
        messData.put("cookingChargePerMeal", cookingCharge);
        messData.put("createdDate", createdDate);
        messData.put("creatorUserId", userId);
        messData.put("lastModified", System.currentTimeMillis());

        // Save to Firebase
        firestore.collection("messes")
                .add(messData)
                .addOnSuccessListener(documentReference -> {
                    String firebaseMessId = documentReference.getId();
                    Log.d(TAG, "Mess created in Firebase: " + firebaseMessId);

                    // Now create in local database
                    MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                        long localMessId = messDao.createMess(messName, groceryBudget, cookingCharge, userId);

                        // Save the firebase ID mapping (we'll use invitation code as the key
                        // identifier)
                        messDao.saveFirebaseMessId((int) localMessId, firebaseMessId, invitationCode);

                        runOnUiThread(() -> {
                            showLoading(false);

                            if (localMessId != -1) {
                                User user = userDao.getUserByIdAsObject((int) userId);

                                if (user != null) {
                                    // Sync user to Firebase (they're now the admin of this mess)
                                    syncManager.syncUserImmediate(user);

                                    prefManager.saveUserSession(
                                            String.valueOf(userId),
                                            String.valueOf(localMessId),
                                            "ADMIN",
                                            user.getFullName(),
                                            user.getEmail());

                                    Toast.makeText(this,
                                            "Mess created! Invitation Code: " + invitationCode,
                                            Toast.LENGTH_LONG).show();

                                    Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Failed to create mess locally", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to create mess in Firebase", e);
                    Toast.makeText(this, "Failed to create mess: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Join mess by looking up invitation code in Firebase
     */
    private void joinMess() {
        String code = etJoinCode.getText().toString().trim();

        // Validation
        if (code.isEmpty()) {
            etJoinCode.setError("Invitation code is required");
            return;
        }

        if (code.length() != 6) {
            etJoinCode.setError("Code must be 6 digits");
            return;
        }

        showLoading(true);
        Log.d(TAG, "Looking up mess with invitation code: '" + code + "'");

        // Look up mess in Firebase by invitation code
        firestore.collection("messes")
                .whereEqualTo("invitationCode", code)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Query returned " + querySnapshot.size() + " results");

                    if (!querySnapshot.isEmpty()) {
                        // Mess found in Firebase
                        DocumentSnapshot messDoc = querySnapshot.getDocuments().get(0);
                        Log.d(TAG, "Found mess: " + messDoc.getId() + ", name: " + messDoc.getString("messName"));
                        joinMessFromFirebase(messDoc, code);
                    } else {
                        // Debug: List all messes to see what codes exist
                        firestore.collection("messes")
                                .get()
                                .addOnSuccessListener(allDocs -> {
                                    Log.d(TAG, "Total messes in Firebase: " + allDocs.size());
                                    for (DocumentSnapshot doc : allDocs.getDocuments()) {
                                        String storedCode = doc.getString("invitationCode");
                                        String messName = doc.getString("messName");
                                        Log.d(TAG, "Mess: " + messName + ", code: '" + storedCode + "'");
                                    }
                                    showLoading(false);
                                    Toast.makeText(this, "Invalid invitation code. Check logs.", Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error looking up mess", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Join mess from Firebase data
     */
    private void joinMessFromFirebase(DocumentSnapshot messDoc, String invitationCode) {
        String firebaseMessId = messDoc.getId();
        String messName = messDoc.getString("messName");
        Double groceryBudget = messDoc.getDouble("groceryBudgetPerMeal");
        Double cookingCharge = messDoc.getDouble("cookingChargePerMeal");
        Long createdDate = messDoc.getLong("createdDate");

        if (groceryBudget == null)
            groceryBudget = 40.0;
        if (cookingCharge == null)
            cookingCharge = 10.0;
        if (createdDate == null)
            createdDate = System.currentTimeMillis() / 1000;

        final double finalGroceryBudget = groceryBudget;
        final double finalCookingCharge = cookingCharge;
        final long finalCreatedDate = createdDate;

        // Create/update mess in local database
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            // Check if mess already exists locally by firebase ID
            int existingMessId = messDao.getMessIdByFirebaseId(firebaseMessId);

            long localMessId;
            if (existingMessId > 0) {
                // Mess already exists locally
                localMessId = existingMessId;
            } else {
                // Create mess locally
                localMessId = messDao.createMessWithDetails(
                        messName, finalGroceryBudget, finalCookingCharge, finalCreatedDate);

                if (localMessId > 0) {
                    messDao.saveFirebaseMessId((int) localMessId, firebaseMessId, invitationCode);
                }
            }

            if (localMessId > 0) {
                // Update user's messId and role
                userDao.updateUserMessId(userId, (int) localMessId);
                userDao.updateUserRole(userId, "member");

                runOnUiThread(() -> {
                    showLoading(false);

                    User user = userDao.getUserByIdAsObject((int) userId);
                    if (user != null) {
                        // Sync user to Firebase (they joined this mess)
                        syncManager.syncUserImmediate(user);

                        prefManager.saveUserSession(
                                String.valueOf(userId),
                                String.valueOf(localMessId),
                                "MEMBER",
                                user.getFullName(),
                                user.getEmail());

                        Toast.makeText(this, "Joined mess successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to join mess locally", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateMess.setEnabled(!show);
        btnJoinMess.setEnabled(!show);
    }
}
