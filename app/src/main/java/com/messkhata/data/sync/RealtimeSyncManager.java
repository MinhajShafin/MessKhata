package com.messkhata.data.sync;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.sync.model.SyncableExpense;
import com.messkhata.data.sync.model.SyncableMeal;
import com.messkhata.data.sync.model.SyncableMess;
import com.messkhata.data.sync.model.SyncableUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages real-time Firestore listeners for automatic data sync
 */
public class RealtimeSyncManager {

    private static final String TAG = "RealtimeSyncManager";

    // Broadcast actions for UI updates
    public static final String ACTION_USERS_UPDATED = "com.messkhata.USERS_UPDATED";
    public static final String ACTION_EXPENSES_UPDATED = "com.messkhata.EXPENSES_UPDATED";
    public static final String ACTION_MEALS_UPDATED = "com.messkhata.MEALS_UPDATED";
    public static final String ACTION_MESS_UPDATED = "com.messkhata.MESS_UPDATED";
    public static final String ACTION_DATA_UPDATED = "com.messkhata.DATA_UPDATED";

    private static RealtimeSyncManager instance;

    private final Context context;
    private final FirebaseFirestore firestore;
    private final UserDao userDao;
    private final ExpenseDao expenseDao;
    private final MealDao mealDao;
    private final MessDao messDao;
    private final ExecutorService executor;

    // Listener registrations (to properly unsubscribe)
    private ListenerRegistration usersListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration mealsListener;
    private ListenerRegistration messListener;

    private String currentFirebaseMessId;
    private int currentLocalMessId;
    private boolean isListening = false;

    private RealtimeSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.userDao = new UserDao(context);
        this.expenseDao = new ExpenseDao(context);
        this.mealDao = new MealDao(context);
        this.messDao = new MessDao(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized RealtimeSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeSyncManager(context);
        }
        return instance;
    }

    /**
     * Start listening for real-time updates for a mess
     */
    public void startListening(int localMessId) {
        String firebaseMessId = messDao.getFirebaseMessId(localMessId);

        if (firebaseMessId == null || firebaseMessId.isEmpty()) {
            Log.w(TAG, "No firebaseMessId found for localMessId: " + localMessId);
            return;
        }

        // Don't restart if already listening to same mess
        if (isListening && firebaseMessId.equals(currentFirebaseMessId)) {
            Log.d(TAG, "Already listening to mess: " + firebaseMessId);
            return;
        }

        // Stop existing listeners first
        stopListening();

        this.currentFirebaseMessId = firebaseMessId;
        this.currentLocalMessId = localMessId;
        this.isListening = true;

        Log.d(TAG, "Starting real-time listeners for firebaseMessId: " + firebaseMessId);

        // Start listening for mess (meal rate changes)
        startMessListener(firebaseMessId);

        // Start listening for users
        startUsersListener(firebaseMessId);

        // Start listening for expenses
        startExpensesListener(firebaseMessId);

        // Start listening for meals
        startMealsListener(firebaseMessId);
    }

    /**
     * Stop all listeners
     */
    public void stopListening() {
        Log.d(TAG, "Stopping all real-time listeners");

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }

        if (mealsListener != null) {
            mealsListener.remove();
            mealsListener = null;
        }

        if (messListener != null) {
            messListener.remove();
            messListener = null;
        }

        isListening = false;
        currentFirebaseMessId = null;
    }

    /**
     * Start listening for mess changes (meal rate updates)
     */
    private void startMessListener(String firebaseMessId) {
        messListener = firestore.collection(SyncableMess.COLLECTION_NAME)
                .document(firebaseMessId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Mess listener error", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Mess changed: " + snapshot.getId());
                        processMessSnapshot(snapshot);
                    }
                });
    }

    /**
     * Start listening for user changes
     */
    private void startUsersListener(String firebaseMessId) {
        usersListener = firestore.collection(SyncableUser.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Users listener error", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Users changed: " + snapshots.size() + " documents");
                        processUsersSnapshot(snapshots);
                    }
                });
    }

    /**
     * Start listening for expense changes
     */
    private void startExpensesListener(String firebaseMessId) {
        expensesListener = firestore.collection(SyncableExpense.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Expenses listener error", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Expenses changed: " + snapshots.size() + " documents");
                        processExpensesSnapshot(snapshots);
                    }
                });
    }

    /**
     * Start listening for meal changes
     */
    private void startMealsListener(String firebaseMessId) {
        mealsListener = firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Meals listener error", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Meals changed: " + snapshots.size() + " documents");
                        processMealsSnapshot(snapshots);
                    }
                });
    }

    /**
     * Process mess snapshot and update local database (meal rates)
     */
    private void processMessSnapshot(DocumentSnapshot snapshot) {
        executor.execute(() -> {
            try {
                SyncableMess mess = SyncableMess.fromFirebaseMap(snapshot.getId(), snapshot.getData());

                // // Get current local mess data to compare timestamps
                // Mess localMess = messDao.getMessByIdAsObject(currentLocalMessId);
                
                // // Only update if Firebase data is newer (has been modified more recently)
                // // This prevents overwriting recent local changes with stale cloud data
                // if (localMess != null && mess.getLastModified() > 0) {
                //     // Check if we have a local modification time (we should add this to Mess table)
                //     // For now, always update from Firebase since we don't track local modification time
                //     Log.d(TAG, "Updating mess rates from Firebase: grocery=" + mess.getGroceryBudgetPerMeal()
                //             + ", cooking=" + mess.getCookingChargePerMeal()
                //             + ", lastModified=" + mess.getLastModified());
                // }

                // Update local mess with new meal rates
                messDao.updateMessRates(
                        currentLocalMessId,
                        mess.getGroceryBudgetPerMeal(),
                        mess.getCookingChargePerMeal());

                Log.d(TAG, "Updated mess rates: grocery=" + mess.getGroceryBudgetPerMeal()
                        + ", cooking=" + mess.getCookingChargePerMeal());

                // Broadcast update
                broadcastUpdate(ACTION_MESS_UPDATED);
                broadcastUpdate(ACTION_DATA_UPDATED);
            } catch (Exception e) {
                Log.e(TAG, "Error processing mess snapshot", e);
            }
        });
    }

    /**
     * Process users snapshot and update local database
     */
    private void processUsersSnapshot(QuerySnapshot snapshots) {
        executor.execute(() -> {
            try {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    SyncableUser user = SyncableUser.fromFirebaseMap(doc.getId(), doc.getData());

                    // Save to local database with local messId
                    userDao.addOrUpdateUser(
                            user.getUserId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhoneNumber(),
                            currentLocalMessId,
                            user.getRole(),
                            user.getJoinedDate());
                }

                // Broadcast update
                broadcastUpdate(ACTION_USERS_UPDATED);
                broadcastUpdate(ACTION_DATA_UPDATED);

                Log.d(TAG, "Processed " + snapshots.size() + " users");
            } catch (Exception e) {
                Log.e(TAG, "Error processing users snapshot", e);
            }
        });
    }

    /**
     * Process expenses snapshot and update local database
     */
    private void processExpensesSnapshot(QuerySnapshot snapshots) {
        executor.execute(() -> {
            try {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    SyncableExpense expense = SyncableExpense.fromFirebaseMap(doc.getId(), doc.getData());

                    // Save to local database with local messId
                    expenseDao.addOrUpdateExpense(
                            expense.getExpenseId(),
                            currentLocalMessId,
                            expense.getAddedBy(),
                            expense.getCategory(),
                            expense.getAmount(),
                            expense.getTitle(),
                            expense.getDescription(),
                            expense.getExpenseDate(),
                            expense.getMemberCountAtTime(),
                            expense.getCreatedAt());
                }

                // Broadcast update
                broadcastUpdate(ACTION_EXPENSES_UPDATED);
                broadcastUpdate(ACTION_DATA_UPDATED);

                Log.d(TAG, "Processed " + snapshots.size() + " expenses");
            } catch (Exception e) {
                Log.e(TAG, "Error processing expenses snapshot", e);
            }
        });
    }

    /**
     * Process meals snapshot and update local database
     */
    private void processMealsSnapshot(QuerySnapshot snapshots) {
        executor.execute(() -> {
            try {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    SyncableMeal meal = SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData());

                    // Resolve userEmail to local userId
                    int localUserId = meal.getUserId();
                    String userEmail = meal.getUserEmail();
                    if (userEmail != null && !userEmail.isEmpty()) {
                        Cursor userCursor = userDao.getUserByEmail(userEmail);
                        if (userCursor != null && userCursor.moveToFirst()) {
                            localUserId = userCursor.getInt(userCursor.getColumnIndexOrThrow("userId"));
                            userCursor.close();
                        }
                    }

                    // Save to local database with resolved userId and local messId
                    mealDao.addOrUpdateMeal(
                            localUserId,
                            currentLocalMessId,
                            meal.getMealDate(),
                            meal.getBreakfast(),
                            meal.getLunch(),
                            meal.getDinner(),
                            meal.getMealRate());
                }

                // Broadcast update
                broadcastUpdate(ACTION_MEALS_UPDATED);
                broadcastUpdate(ACTION_DATA_UPDATED);

                Log.d(TAG, "Processed " + snapshots.size() + " meals");
            } catch (Exception e) {
                Log.e(TAG, "Error processing meals snapshot", e);
            }
        });
    }

    /**
     * Send local broadcast to notify UI components
     */
    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Check if currently listening
     */
    public boolean isListening() {
        return isListening;
    }
}
