package com.messkhata.sync;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MemberBalanceDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.MonthlyReportDao;
import com.messkhata.data.dao.NotificationDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Expense;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.data.model.Notification;
import com.messkhata.data.model.User;
import com.messkhata.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Sync Manager handles real-time synchronization between
 * local SQLite database and Firebase Firestore.
 *
 * Features:
 * - Real-time listeners for incoming changes
 * - Offline data caching and upload when online
 * - Conflict resolution using timestamp-based last-write-wins
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";

    private static FirebaseSyncManager instance;
    private final FirebaseFirestore firestore;
    private final MessKhataDatabase database;

    private final List<ListenerRegistration> activeListeners = new ArrayList<>();
    private String currentMessId;
    private String currentUserId;

    private FirebaseSyncManager(MessKhataDatabase database) {
        this.firestore = FirebaseFirestore.getInstance();
        this.database = database;
    }

    public static synchronized FirebaseSyncManager getInstance(MessKhataDatabase database) {
        if (instance == null) {
            instance = new FirebaseSyncManager(database);
        }
        return instance;
    }

    /**
     * Initialize sync for a specific mess and user.
     */
    public void initSync(String messId, String userId) {
        this.currentMessId = messId;
        this.currentUserId = userId;

        // Start real-time listeners
        startMealListener();
        startExpenseListener();
        startUserListener();
        startReportListener();
        startNotificationListener();

        // Upload any pending local changes
        uploadPendingChanges();
    }

    /**
     * Stop all active listeners.
     */
    public void stopSync() {
        for (ListenerRegistration registration : activeListeners) {
            registration.remove();
        }
        activeListeners.clear();
    }

    // ==================== REAL-TIME LISTENERS ====================

    private void startMealListener() {
        if (currentMessId == null) return;

        ListenerRegistration registration = firestore
                .collection(Constants.COLLECTION_MESSES)
                .document(currentMessId)
                .collection(Constants.COLLECTION_MEALS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Meal listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        handleMealChanges(snapshots);
                    }
                });
        activeListeners.add(registration);
    }

    private void startExpenseListener() {
        if (currentMessId == null) return;

        ListenerRegistration registration = firestore
                .collection(Constants.COLLECTION_MESSES)
                .document(currentMessId)
                .collection(Constants.COLLECTION_EXPENSES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Expense listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        handleExpenseChanges(snapshots);
                    }
                });
        activeListeners.add(registration);
    }

    private void startUserListener() {
        if (currentMessId == null) return;

        ListenerRegistration registration = firestore
                .collection(Constants.COLLECTION_USERS)
                .whereEqualTo("messId", currentMessId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "User listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        handleUserChanges(snapshots);
                    }
                });
        activeListeners.add(registration);
    }

    private void startReportListener() {
        if (currentMessId == null) return;

        ListenerRegistration registration = firestore
                .collection(Constants.COLLECTION_MESSES)
                .document(currentMessId)
                .collection(Constants.COLLECTION_REPORTS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Report listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        handleReportChanges(snapshots);
                    }
                });
        activeListeners.add(registration);
    }

    private void startNotificationListener() {
        if (currentUserId == null) return;

        ListenerRegistration registration = firestore
                .collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Notification listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        handleNotificationChanges(snapshots);
                    }
                });
        activeListeners.add(registration);
    }

    // ==================== HANDLE INCOMING CHANGES ====================

    private void handleMealChanges(QuerySnapshot snapshots) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            MealDao mealDao = database.mealDao();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Meal remoteMeal = doc.toObject(Meal.class);
                if (remoteMeal == null) continue;

                Meal localMeal = mealDao.getMealByIdSync(remoteMeal.getId());

                if (localMeal == null) {
                    // New meal from server
                    remoteMeal.setSynced(true);
                    remoteMeal.setPendingAction(null);
                    mealDao.insert(remoteMeal);
                } else if (!localMeal.isSynced()) {
                    // Conflict: use timestamp-based resolution
                    if (remoteMeal.getUpdatedAt() > localMeal.getUpdatedAt()) {
                        remoteMeal.setSynced(true);
                        remoteMeal.setPendingAction(null);
                        mealDao.insert(remoteMeal);
                    }
                    // If local is newer, keep local and let upload handle it
                } else {
                    // Update local with server data
                    remoteMeal.setSynced(true);
                    remoteMeal.setPendingAction(null);
                    mealDao.insert(remoteMeal);
                }
            }
        });
    }

    private void handleExpenseChanges(QuerySnapshot snapshots) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            ExpenseDao expenseDao = database.expenseDao();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Expense remoteExpense = doc.toObject(Expense.class);
                if (remoteExpense == null) continue;

                Expense localExpense = expenseDao.getExpenseByIdSync(remoteExpense.getId());

                if (localExpense == null) {
                    remoteExpense.setSynced(true);
                    remoteExpense.setPendingAction(null);
                    expenseDao.insert(remoteExpense);
                } else if (!localExpense.isSynced()) {
                    if (remoteExpense.getUpdatedAt() > localExpense.getUpdatedAt()) {
                        remoteExpense.setSynced(true);
                        remoteExpense.setPendingAction(null);
                        expenseDao.insert(remoteExpense);
                    }
                } else {
                    remoteExpense.setSynced(true);
                    remoteExpense.setPendingAction(null);
                    expenseDao.insert(remoteExpense);
                }
            }
        });
    }

    private void handleUserChanges(QuerySnapshot snapshots) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            UserDao userDao = database.userDao();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                User remoteUser = doc.toObject(User.class);
                if (remoteUser == null) continue;

                User localUser = userDao.getUserByIdSync(remoteUser.getId());

                if (localUser == null) {
                    remoteUser.setSynced(true);
                    remoteUser.setPendingAction(null);
                    userDao.insert(remoteUser);
                } else if (!localUser.isSynced()) {
                    if (remoteUser.getUpdatedAt() > localUser.getUpdatedAt()) {
                        remoteUser.setSynced(true);
                        remoteUser.setPendingAction(null);
                        userDao.insert(remoteUser);
                    }
                } else {
                    remoteUser.setSynced(true);
                    remoteUser.setPendingAction(null);
                    userDao.insert(remoteUser);
                }
            }
        });
    }

    private void handleReportChanges(QuerySnapshot snapshots) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            MonthlyReportDao reportDao = database.monthlyReportDao();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                MonthlyReport remoteReport = doc.toObject(MonthlyReport.class);
                if (remoteReport == null) continue;

                MonthlyReport localReport = reportDao.getReportByIdSync(remoteReport.getId());

                if (localReport == null) {
                    remoteReport.setSynced(true);
                    remoteReport.setPendingAction(null);
                    reportDao.insert(remoteReport);
                } else {
                    remoteReport.setSynced(true);
                    remoteReport.setPendingAction(null);
                    reportDao.insert(remoteReport);
                }
            }
        });
    }

    private void handleNotificationChanges(QuerySnapshot snapshots) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            NotificationDao notificationDao = database.notificationDao();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Notification remoteNotif = doc.toObject(Notification.class);
                if (remoteNotif == null) continue;

                remoteNotif.setSynced(true);
                remoteNotif.setPendingAction(null);
                notificationDao.insert(remoteNotif);
            }
        });
    }

    // ==================== UPLOAD PENDING CHANGES ====================

    /**
     * Upload all pending local changes to Firebase.
     */
    public void uploadPendingChanges() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            uploadPendingMeals();
            uploadPendingExpenses();
            uploadPendingUsers();
            uploadPendingReports();
        });
    }

    private void uploadPendingMeals() {
        MealDao mealDao = database.mealDao();
        List<Meal> unsyncedMeals = mealDao.getUnsyncedMeals();

        for (Meal meal : unsyncedMeals) {
            String action = meal.getPendingAction();
            String path = Constants.COLLECTION_MESSES + "/" + meal.getMessId() +
                         "/" + Constants.COLLECTION_MEALS + "/" + meal.getId();

            if (Constants.ACTION_DELETE.equals(action)) {
                firestore.document(path)
                        .delete()
                        .addOnSuccessListener(v -> {
                            MessKhataDatabase.databaseWriteExecutor.execute(() ->
                                mealDao.deleteById(meal.getId())
                            );
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete meal", e));
            } else {
                Map<String, Object> mealData = mealToMap(meal);
                firestore.document(path)
                        .set(mealData)
                        .addOnSuccessListener(v -> {
                            MessKhataDatabase.databaseWriteExecutor.execute(() ->
                                mealDao.markAsSynced(meal.getId())
                            );
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to sync meal", e));
            }
        }
    }

    private void uploadPendingExpenses() {
        ExpenseDao expenseDao = database.expenseDao();
        List<Expense> unsyncedExpenses = expenseDao.getUnsyncedExpenses();

        for (Expense expense : unsyncedExpenses) {
            String action = expense.getPendingAction();
            String path = Constants.COLLECTION_MESSES + "/" + expense.getMessId() +
                         "/" + Constants.COLLECTION_EXPENSES + "/" + expense.getId();

            if (Constants.ACTION_DELETE.equals(action)) {
                firestore.document(path)
                        .delete()
                        .addOnSuccessListener(v -> {
                            MessKhataDatabase.databaseWriteExecutor.execute(() ->
                                expenseDao.deleteById(expense.getId())
                            );
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete expense", e));
            } else {
                Map<String, Object> expenseData = expenseToMap(expense);
                firestore.document(path)
                        .set(expenseData)
                        .addOnSuccessListener(v -> {
                            MessKhataDatabase.databaseWriteExecutor.execute(() ->
                                expenseDao.markAsSynced(expense.getId())
                            );
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to sync expense", e));
            }
        }
    }

    private void uploadPendingUsers() {
        UserDao userDao = database.userDao();
        List<User> unsyncedUsers = userDao.getUnsyncedUsers();

        for (User user : unsyncedUsers) {
            String path = Constants.COLLECTION_USERS + "/" + user.getId();
            Map<String, Object> userData = userToMap(user);

            firestore.document(path)
                    .set(userData)
                    .addOnSuccessListener(v -> {
                        MessKhataDatabase.databaseWriteExecutor.execute(() ->
                            userDao.markAsSynced(user.getId())
                        );
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to sync user", e));
        }
    }

    private void uploadPendingReports() {
        MonthlyReportDao reportDao = database.monthlyReportDao();
        List<MonthlyReport> unsyncedReports = reportDao.getUnsyncedReports();

        for (MonthlyReport report : unsyncedReports) {
            String path = Constants.COLLECTION_MESSES + "/" + report.getMessId() +
                         "/" + Constants.COLLECTION_REPORTS + "/" + report.getId();
            Map<String, Object> reportData = reportToMap(report);

            firestore.document(path)
                    .set(reportData)
                    .addOnSuccessListener(v -> {
                        MessKhataDatabase.databaseWriteExecutor.execute(() ->
                            reportDao.markAsSynced(report.getId())
                        );
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to sync report", e));
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> mealToMap(Meal meal) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", meal.getId());
        map.put("messId", meal.getMessId());
        map.put("userId", meal.getUserId());
        map.put("userName", meal.getUserName());
        map.put("date", meal.getDate());
        map.put("mealType", meal.getMealType());
        map.put("count", meal.getCount());
        map.put("notes", meal.getNotes());
        map.put("createdAt", meal.getCreatedAt());
        map.put("updatedAt", meal.getUpdatedAt());
        map.put("createdBy", meal.getCreatedBy());
        return map;
    }

    private Map<String, Object> expenseToMap(Expense expense) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", expense.getId());
        map.put("messId", expense.getMessId());
        map.put("category", expense.getCategory());
        map.put("description", expense.getDescription());
        map.put("amount", expense.getAmount());
        map.put("date", expense.getDate());
        map.put("paidById", expense.getPaidById());
        map.put("paidByName", expense.getPaidByName());
        map.put("receiptUrl", expense.getReceiptUrl());
        map.put("isSharedEqually", expense.isSharedEqually());
        map.put("isIncludedInMealRate", expense.isIncludedInMealRate());
        map.put("createdAt", expense.getCreatedAt());
        map.put("updatedAt", expense.getUpdatedAt());
        return map;
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        map.put("messId", user.getMessId());
        map.put("isActive", user.isActive());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }

    private Map<String, Object> reportToMap(MonthlyReport report) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", report.getId());
        map.put("messId", report.getMessId());
        map.put("year", report.getYear());
        map.put("month", report.getMonth());
        map.put("totalMeals", report.getTotalMeals());
        map.put("totalMealExpenses", report.getTotalMealExpenses());
        map.put("totalFixedExpenses", report.getTotalFixedExpenses());
        map.put("mealRate", report.getMealRate());
        map.put("isFinalized", report.isFinalized());
        map.put("createdAt", report.getCreatedAt());
        map.put("updatedAt", report.getUpdatedAt());
        return map;
    }

    /**
     * Create a new mess in Firebase.
     */
    public void createMess(Mess mess, OnSyncCompleteListener listener) {
        String path = Constants.COLLECTION_MESSES + "/" + mess.getId();
        Map<String, Object> messData = new HashMap<>();
        messData.put("id", mess.getId());
        messData.put("name", mess.getName());
        messData.put("address", mess.getAddress());
        messData.put("adminId", mess.getAdminId());
        messData.put("joinCode", mess.getJoinCode());
        messData.put("memberCount", mess.getMemberCount());
        messData.put("createdAt", mess.getCreatedAt());
        messData.put("updatedAt", mess.getUpdatedAt());

        firestore.document(path)
                .set(messData)
                .addOnSuccessListener(v -> {
                    MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                        database.messDao().markAsSynced(mess.getId());
                    });
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Find a mess by join code.
     */
    public void findMessByJoinCode(String joinCode, OnMessFoundListener listener) {
        firestore.collection(Constants.COLLECTION_MESSES)
                .whereEqualTo("joinCode", joinCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Mess mess = querySnapshot.getDocuments().get(0).toObject(Mess.class);
                        listener.onFound(mess);
                    } else {
                        listener.onNotFound();
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // ==================== LISTENER INTERFACES ====================

    public interface OnSyncCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnMessFoundListener {
        void onFound(Mess mess);
        void onNotFound();
        void onError(String error);
    }
}
