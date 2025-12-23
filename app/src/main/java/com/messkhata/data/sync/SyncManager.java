package com.messkhata.data.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.model.Expense;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.User;
import com.messkhata.data.sync.model.SyncableExpense;
import com.messkhata.data.sync.model.SyncableMeal;
import com.messkhata.data.sync.model.SyncableMess;
import com.messkhata.data.sync.model.SyncableUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages synchronization between local SQLite database and Firebase Firestore
 * Implements a "local-first" strategy with periodic cloud sync
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String PREFS_NAME = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";

    private static SyncManager instance;

    private final Context context;
    private final FirebaseRepository firebaseRepo;
    private final SharedPreferences syncPrefs;
    private final ExecutorService executor;

    // DAOs for local database
    private final UserDao userDao;
    private final MessDao messDao;
    private final MealDao mealDao;
    private final ExpenseDao expenseDao;

    private boolean isSyncing = false;
    private SyncCallback syncCallback;

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseRepo = FirebaseRepository.getInstance();
        this.syncPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();

        // Initialize DAOs
        this.userDao = new UserDao(context);
        this.messDao = new MessDao(context);
        this.mealDao = new MealDao(context);
        this.expenseDao = new ExpenseDao(context);
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    /**
     * Set callback for sync status updates
     */
    public void setSyncCallback(SyncCallback callback) {
        this.syncCallback = callback;
    }

    /**
     * Check if sync is enabled
     */
    public boolean isSyncEnabled() {
        return syncPrefs.getBoolean(KEY_SYNC_ENABLED, true);
    }

    /**
     * Enable or disable sync
     */
    public void setSyncEnabled(boolean enabled) {
        syncPrefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
    }

    /**
     * Get last sync timestamp
     */
    public long getLastSyncTimestamp() {
        return syncPrefs.getLong(KEY_LAST_SYNC, 0);
    }

    /**
     * Update last sync timestamp
     */
    private void updateLastSyncTimestamp() {
        syncPrefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
    }

    /**
     * Check if device is connected to network
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * Check if user is authenticated with Firebase
     */
    public boolean isAuthenticated() {
        return firebaseRepo.isAuthenticated();
    }

    /**
     * Check if sync is currently in progress
     */
    public boolean isSyncing() {
        return isSyncing;
    }

    /**
     * Perform full sync (upload local changes, download remote changes)
     *
     * @param messId The mess ID to sync
     */
    public void performFullSync(int messId) {
        if (!isSyncEnabled()) {
            Log.d(TAG, "Sync is disabled");
            return;
        }

        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network connection");
            if (syncCallback != null) {
                syncCallback.onSyncFailed("No network connection");
            }
            return;
        }

        if (!isAuthenticated()) {
            Log.d(TAG, "User not authenticated");
            if (syncCallback != null) {
                syncCallback.onSyncFailed("Please sign in to sync");
            }
            return;
        }

        if (isSyncing) {
            Log.d(TAG, "Sync already in progress");
            return;
        }

        isSyncing = true;

        if (syncCallback != null) {
            syncCallback.onSyncStarted();
        }

        executor.execute(() -> {
            try {
                // Step 1: Upload local mess data
                notifyProgress(10, "Syncing mess data...");
                syncMessToCloud(messId);

                // Step 2: Upload local users
                notifyProgress(25, "Syncing users...");
                syncUsersToCloud(messId);

                // Step 3: Upload local meals
                notifyProgress(40, "Syncing meals...");
                syncMealsToCloud(messId);

                // Step 4: Upload local expenses
                notifyProgress(55, "Syncing expenses...");
                syncExpensesToCloud(messId);

                // Step 5: Download remote changes
                notifyProgress(70, "Downloading updates...");
                downloadRemoteChanges(messId);

                // Step 6: Complete
                notifyProgress(100, "Sync completed");
                updateLastSyncTimestamp();

                if (syncCallback != null) {
                    syncCallback.onSyncCompleted();
                }

                Log.d(TAG, "Full sync completed successfully");

            } catch (Exception e) {
                Log.e(TAG, "Sync failed", e);
                if (syncCallback != null) {
                    syncCallback.onSyncFailed(e.getMessage());
                }
            } finally {
                isSyncing = false;
            }
        });
    }

    /**
     * Sync mess data to cloud
     */
    private void syncMessToCloud(int messId) {
        try {
            Cursor cursor = messDao.getMessById(messId);
            if (cursor != null && cursor.moveToFirst()) {
                Mess mess = new Mess(
                        cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("messName")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("groceryBudgetPerMeal")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("cookingChargePerMeal")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("createdDate")));
                cursor.close();

                SyncableMess syncableMess = new SyncableMess(mess);
                syncableMess.setLastModified(System.currentTimeMillis());

                Task<DocumentReference> task = firebaseRepo.saveMess(syncableMess);
                Tasks.await(task);

                Log.d(TAG, "Mess synced to cloud: " + messId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing mess", e);
        }
    }

    /**
     * Sync users to cloud
     */
    private void syncUsersToCloud(int messId) {
        try {
            // Get firebaseMessId for proper cross-device sync
            String firebaseMessId = messDao.getFirebaseMessId(messId);
            Log.d(TAG, "syncUsersToCloud - messId: " + messId + ", firebaseMessId: " + firebaseMessId);

            Cursor cursor = userDao.getUsersByMessId(messId);
            if (cursor != null) {
                int userCount = 0;
                while (cursor.moveToNext()) {
                    User user = new User(
                            cursor.getLong(cursor.getColumnIndexOrThrow("userId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                            cursor.getString(cursor.getColumnIndexOrThrow("email")),
                            cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("role")),
                            cursor.getLong(cursor.getColumnIndexOrThrow("joinedDate")));

                    SyncableUser syncableUser = new SyncableUser(user);
                    syncableUser.setLastModified(System.currentTimeMillis());
                    // Set firebaseMessId for cross-device sync
                    if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
                        syncableUser.setFirebaseMessId(firebaseMessId);
                    }

                    Log.d(TAG, "Uploading user: " + user.getEmail() + " with firebaseMessId: " + firebaseMessId);

                    Task<DocumentReference> task = firebaseRepo.saveUser(syncableUser);
                    Tasks.await(task);
                    userCount++;
                }
                cursor.close();

                Log.d(TAG, "Users synced to cloud for mess: " + messId + ", total: " + userCount);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing users", e);
        }
    }

    /**
     * Sync meals to cloud
     */
    private void syncMealsToCloud(int messId) {
        try {
            // Get firebaseMessId for proper cross-device sync
            String firebaseMessId = messDao.getFirebaseMessId(messId);

            // Get all meals for the mess (we'll need to add a method to MealDao)
            // For now, sync current month's meals
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
            int currentYear = cal.get(java.util.Calendar.YEAR);

            // Get all users in mess
            Cursor userCursor = userDao.getUsersByMessId(messId);
            if (userCursor != null) {
                List<SyncableMeal> allMeals = new ArrayList<>();

                while (userCursor.moveToNext()) {
                    int userId = userCursor.getInt(userCursor.getColumnIndexOrThrow("userId"));
                    String userEmail = userCursor.getString(userCursor.getColumnIndexOrThrow("email"));
                    List<Meal> meals = mealDao.getMealsByMonth(userId, currentMonth, currentYear);

                    for (Meal meal : meals) {
                        SyncableMeal syncableMeal = new SyncableMeal(meal);
                        syncableMeal.setLastModified(System.currentTimeMillis());
                        syncableMeal.setUserEmail(userEmail); // For cross-device user matching
                        // Set firebaseMessId for cross-device sync
                        if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
                            syncableMeal.setFirebaseMessId(firebaseMessId);
                        }
                        allMeals.add(syncableMeal);
                    }
                }
                userCursor.close();

                // Batch upload meals
                if (!allMeals.isEmpty()) {
                    Task<Void> task = firebaseRepo.saveMealsBatch(allMeals);
                    Tasks.await(task);
                    Log.d(TAG, "Meals synced to cloud: " + allMeals.size());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing meals", e);
        }
    }

    /**
     * Sync expenses to cloud
     */
    private void syncExpensesToCloud(int messId) {
        try {
            // Get firebaseMessId for proper cross-device sync
            String firebaseMessId = messDao.getFirebaseMessId(messId);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
            int currentYear = cal.get(java.util.Calendar.YEAR);

            List<Expense> expenses = expenseDao.getExpensesByMonth(messId, currentMonth, currentYear);
            List<SyncableExpense> syncableExpenses = new ArrayList<>();

            for (Expense expense : expenses) {
                SyncableExpense syncableExpense = new SyncableExpense(expense);
                syncableExpense.setLastModified(System.currentTimeMillis());
                // Set firebaseMessId for cross-device sync
                if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
                    syncableExpense.setFirebaseMessId(firebaseMessId);
                }
                syncableExpenses.add(syncableExpense);
            }

            if (!syncableExpenses.isEmpty()) {
                Task<Void> task = firebaseRepo.saveExpensesBatch(syncableExpenses);
                Tasks.await(task);
                Log.d(TAG, "Expenses synced to cloud: " + syncableExpenses.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing expenses", e);
        }
    }

    /**
     * Download remote changes from cloud
     */
    private void downloadRemoteChanges(int messId) {
        // Get firebaseMessId for proper cross-device sync
        String firebaseMessId = messDao.getFirebaseMessId(messId);
        if (firebaseMessId == null || firebaseMessId.isEmpty()) {
            Log.w(TAG, "No firebaseMessId found for messId: " + messId + ", falling back to local messId");
            // Fall back to old behavior if firebaseMessId not available
            downloadRemoteChangesLegacy(messId, 0);
            return;
        }

        Log.d(TAG, "Downloading remote changes using firebaseMessId: " + firebaseMessId);

        // IMPORTANT: Download users FIRST - this is the most critical for member count
        // This query doesn't need a composite index (single field query)
        try {
            Log.d(TAG, "Querying users with firebaseMessId: " + firebaseMessId);
            Task<List<SyncableUser>> usersTask = firebaseRepo.getUsersByFirebaseMessId(firebaseMessId);
            List<SyncableUser> remoteUsers = Tasks.await(usersTask);

            Log.d(TAG, "Firebase returned " + remoteUsers.size() + " users for firebaseMessId: " + firebaseMessId);

            for (SyncableUser user : remoteUsers) {
                Log.d(TAG, "Processing remote user: " + user.getEmail() + ", role: " + user.getRole() +
                        ", firebaseMessId: " + user.getFirebaseMessId());
                // Save to local database - use local messId
                userDao.addOrUpdateUser(
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        messId, // Use local messId for local database
                        user.getRole(),
                        user.getJoinedDate());
            }
            Log.d(TAG, "Downloaded and saved " + remoteUsers.size() + " users from cloud");

            // Also try to get local user count for comparison
            try {
                android.database.Cursor localUsersCursor = userDao.getUsersByMessId(messId);
                int localCount = localUsersCursor != null ? localUsersCursor.getCount() : 0;
                if (localUsersCursor != null)
                    localUsersCursor.close();
                Log.d(TAG, "Local database now has " + localCount + " users for messId: " + messId);
            } catch (Exception ex) {
                Log.e(TAG, "Error counting local users", ex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading users", e);
        }

        // Download ALL meals for this mess (simple query, no index required)
        try {
            Log.d(TAG, "Downloading all meals for firebaseMessId: " + firebaseMessId);
            Task<List<SyncableMeal>> mealsTask = firebaseRepo.getAllMealsByFirebaseMessId(firebaseMessId);
            List<SyncableMeal> remoteMeals = Tasks.await(mealsTask);

            for (SyncableMeal meal : remoteMeals) {
                // Resolve userEmail to local userId
                int localUserId = meal.getUserId(); // Default to remote userId
                String userEmail = meal.getUserEmail();
                if (userEmail != null && !userEmail.isEmpty()) {
                    Cursor userCursor = userDao.getUserByEmail(userEmail);
                    if (userCursor != null && userCursor.moveToFirst()) {
                        localUserId = userCursor.getInt(userCursor.getColumnIndexOrThrow("userId"));
                        userCursor.close();
                    }
                }

                // Update local database with remote changes - use local messId and resolved
                // userId
                mealDao.addOrUpdateMeal(
                        localUserId,
                        messId, // Use local messId for local database
                        meal.getMealDate(),
                        meal.getBreakfast(),
                        meal.getLunch(),
                        meal.getDinner(),
                        meal.getMealRate());
            }
            Log.d(TAG, "Downloaded " + remoteMeals.size() + " meals from cloud");
        } catch (Exception e) {
            Log.e(TAG, "Error downloading meals: " + e.getMessage(), e);
        }

        // Download ALL expenses for this mess (simple query, no index required)
        try {
            Log.d(TAG, "Downloading all expenses for firebaseMessId: " + firebaseMessId);
            Task<List<SyncableExpense>> expensesTask = firebaseRepo.getAllExpensesByFirebaseMessId(firebaseMessId);
            List<SyncableExpense> remoteExpenses = Tasks.await(expensesTask);

            Log.d(TAG, "Firebase returned " + remoteExpenses.size() + " expenses");

            for (SyncableExpense expense : remoteExpenses) {
                Log.d(TAG, "Processing expense: " + expense.getTitle() + ", amount: " + expense.getAmount());
                // Save to local database - use local messId
                expenseDao.addOrUpdateExpense(
                        expense.getExpenseId(),
                        messId, // Use local messId for local database
                        expense.getAddedBy(),
                        expense.getCategory(),
                        expense.getAmount(),
                        expense.getTitle(),
                        expense.getDescription(),
                        expense.getExpenseDate(),
                        expense.getMemberCountAtTime(),
                        expense.getCreatedAt());
            }
            Log.d(TAG, "Downloaded and saved " + remoteExpenses.size() + " expenses from cloud");
        } catch (Exception e) {
            Log.e(TAG, "Error downloading expenses: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy download method using local messId (for backward compatibility)
     */
    @SuppressWarnings("deprecation")
    private void downloadRemoteChangesLegacy(int messId, long lastSync) {
        try {
            // Download meals modified after last sync
            Task<List<SyncableMeal>> mealsTask = firebaseRepo.getMealsModifiedAfter(messId, lastSync);
            List<SyncableMeal> remoteMeals = Tasks.await(mealsTask);

            for (SyncableMeal meal : remoteMeals) {
                mealDao.addOrUpdateMeal(
                        meal.getUserId(),
                        meal.getMessId(),
                        meal.getMealDate(),
                        meal.getBreakfast(),
                        meal.getLunch(),
                        meal.getDinner(),
                        meal.getMealRate());
            }
            Log.d(TAG, "[Legacy] Downloaded " + remoteMeals.size() + " meals from cloud");

            // Download expenses modified after last sync
            Task<List<SyncableExpense>> expensesTask = firebaseRepo.getExpensesModifiedAfter(messId, lastSync);
            List<SyncableExpense> remoteExpenses = Tasks.await(expensesTask);

            for (SyncableExpense expense : remoteExpenses) {
                expenseDao.addOrUpdateExpense(
                        expense.getExpenseId(),
                        expense.getMessId(),
                        expense.getAddedBy(),
                        expense.getCategory(),
                        expense.getAmount(),
                        expense.getTitle(),
                        expense.getDescription(),
                        expense.getExpenseDate(),
                        expense.getMemberCountAtTime(),
                        expense.getCreatedAt());
            }
            Log.d(TAG, "[Legacy] Downloaded and saved " + remoteExpenses.size() + " expenses from cloud");

            // Download users for this mess
            Task<List<SyncableUser>> usersTask = firebaseRepo.getUsersByMessId(messId);
            List<SyncableUser> remoteUsers = Tasks.await(usersTask);

            for (SyncableUser user : remoteUsers) {
                userDao.addOrUpdateUser(
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getMessId(),
                        user.getRole(),
                        user.getJoinedDate());
            }
            Log.d(TAG, "[Legacy] Downloaded and saved " + remoteUsers.size() + " users from cloud");

        } catch (Exception e) {
            Log.e(TAG, "Error in legacy download", e);
        }
    }

    /**
     * Sync a single meal immediately
     * If offline, queues the operation for later
     */
    public void syncMealImmediate(Meal meal) {
        if (!isSyncEnabled() || !isAuthenticated()) {
            return;
        }

        // Get sync data ready
        SyncableMeal syncableMeal = new SyncableMeal(meal);
        syncableMeal.setLastModified(System.currentTimeMillis());

        // Set firebaseMessId for cross-device sync
        String firebaseMessId = messDao.getFirebaseMessId(meal.getMessId());
        if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
            syncableMeal.setFirebaseMessId(firebaseMessId);
        }

        // Set userEmail for cross-device user matching
        User user = userDao.getUserByIdAsObject(meal.getUserId());
        if (user != null && user.getEmail() != null) {
            syncableMeal.setUserEmail(user.getEmail());
        }

        // If offline, queue the operation
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline - queuing meal sync for later");
            OfflineQueueManager.getInstance(context).queueOperation(
                    OfflineQueueManager.OP_UPDATE,
                    OfflineQueueManager.ENTITY_MEAL,
                    String.valueOf(meal.getMealId()),
                    syncableMeal.getFirebaseId(),
                    firebaseMessId,
                    syncableMeal);
            return;
        }

        final String finalFirebaseMessId = firebaseMessId;
        final int totalMeals = meal.getBreakfast() + meal.getLunch() + meal.getDinner();
        executor.execute(() -> {
            try {
                Task<DocumentReference> task = firebaseRepo.saveMeal(syncableMeal);
                Tasks.await(task);

                Log.d(TAG, "Meal synced immediately: " + meal.getMealId());
            } catch (Exception e) {
                Log.e(TAG, "Error syncing meal immediately", e);
                // Queue for retry if sync fails
                OfflineQueueManager.getInstance(context).queueOperation(
                        OfflineQueueManager.OP_UPDATE,
                        OfflineQueueManager.ENTITY_MEAL,
                        String.valueOf(meal.getMealId()),
                        syncableMeal.getFirebaseId(),
                        finalFirebaseMessId,
                        syncableMeal);
            }
        });
    }

    /**
     * Sync a single expense immediately
     * If offline, queues the operation for later
     */
    public void syncExpenseImmediate(Expense expense) {
        if (!isSyncEnabled() || !isAuthenticated()) {
            return;
        }

        // Get sync data ready
        SyncableExpense syncableExpense = new SyncableExpense(expense);
        syncableExpense.setLastModified(System.currentTimeMillis());

        // Set firebaseMessId for cross-device sync
        String firebaseMessId = messDao.getFirebaseMessId(expense.getMessId());
        if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
            syncableExpense.setFirebaseMessId(firebaseMessId);
        }

        // If offline, queue the operation
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline - queuing expense sync for later");
            OfflineQueueManager.getInstance(context).queueOperation(
                    OfflineQueueManager.OP_CREATE,
                    OfflineQueueManager.ENTITY_EXPENSE,
                    String.valueOf(expense.getExpenseId()),
                    null,
                    firebaseMessId,
                    syncableExpense);
            return;
        }

        final String finalFirebaseMessId = firebaseMessId;
        executor.execute(() -> {
            try {
                Task<DocumentReference> task = firebaseRepo.saveExpense(syncableExpense);
                Tasks.await(task);

                Log.d(TAG, "Expense synced immediately: " + expense.getExpenseId());
            } catch (Exception e) {
                Log.e(TAG, "Error syncing expense immediately", e);
                // Queue for retry if sync fails
                OfflineQueueManager.getInstance(context).queueOperation(
                        OfflineQueueManager.OP_CREATE,
                        OfflineQueueManager.ENTITY_EXPENSE,
                        String.valueOf(expense.getExpenseId()),
                        null,
                        finalFirebaseMessId,
                        syncableExpense);
            }
        });
    }

    /**
     * Sync a single user immediately (used when joining/creating mess)
     */
    public void syncUserImmediate(User user) {
        syncUserImmediate(user, null);
    }

    /**
     * Sync a single user immediately with firebaseMessId
     *
     * @param user           The user to sync
     * @param firebaseMessId The Firebase document ID of the mess (optional)
     */
    public void syncUserImmediate(User user, String firebaseMessId) {
        if (!isNetworkAvailable() || !isAuthenticated()) {
            return;
        }

        executor.execute(() -> {
            try {
                SyncableUser syncableUser = new SyncableUser(user);
                syncableUser.setLastModified(System.currentTimeMillis());

                // Set firebaseMessId if provided
                if (firebaseMessId != null && !firebaseMessId.isEmpty()) {
                    syncableUser.setFirebaseMessId(firebaseMessId);
                }

                Log.d(TAG, "syncUserImmediate - user: " + user.getEmail() + ", firebaseMessId: " + firebaseMessId);

                Task<DocumentReference> task = firebaseRepo.saveUser(syncableUser);
                DocumentReference docRef = Tasks.await(task);

                Log.d(TAG, "User synced immediately: " + user.getUserId() + ", docId: "
                        + (docRef != null ? docRef.getId() : "null"));
            } catch (Exception e) {
                Log.e(TAG, "Error syncing user immediately", e);
            }
        });
    }

    /**
     * Sync mess data immediately (used when meal rate is updated)
     */
    public void syncMessImmediate(int messId) {
        if (!isSyncEnabled() || !isAuthenticated()) {
            return;
        }

        // If offline, queue the operation
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline - queuing mess sync for later");
            // Get mess data for queuing
            Cursor cursor = messDao.getMessById(messId);
            if (cursor != null && cursor.moveToFirst()) {
                Mess mess = new Mess(
                        cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("messName")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("groceryBudgetPerMeal")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("cookingChargePerMeal")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("createdDate")));
                cursor.close();

                String firebaseMessId = messDao.getFirebaseMessId(messId);
                SyncableMess syncableMess = new SyncableMess(mess);
                syncableMess.setFirebaseId(firebaseMessId);
                syncableMess.setLastModified(System.currentTimeMillis());

                OfflineQueueManager.getInstance(context).queueOperation(
                        OfflineQueueManager.OP_UPDATE,
                        OfflineQueueManager.ENTITY_MESS,
                        String.valueOf(messId),
                        firebaseMessId,
                        firebaseMessId,
                        syncableMess);
            }
            return;
        }

        executor.execute(() -> {
            try {
                syncMessToCloud(messId);
                Log.d(TAG, "Mess synced immediately: " + messId);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing mess immediately", e);
            }
        });
    }

    /**
     * Helper method to notify progress
     */
    private void notifyProgress(int progress, String message) {
        if (syncCallback != null) {
            syncCallback.onSyncProgress(progress, message);
        }
    }
}
