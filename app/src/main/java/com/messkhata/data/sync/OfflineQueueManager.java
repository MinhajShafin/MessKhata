package com.messkhata.data.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.google.gson.Gson;
import com.messkhata.data.sync.model.SyncableExpense;
import com.messkhata.data.sync.model.SyncableMeal;
import com.messkhata.data.sync.model.SyncableMess;
import com.messkhata.data.sync.model.SyncableUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages offline queue for sync operations.
 * Queues changes when offline and processes them when connection is restored.
 */
public class OfflineQueueManager extends SQLiteOpenHelper {

    private static final String TAG = "OfflineQueueManager";
    private static final String DATABASE_NAME = "offline_queue.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_QUEUE = "sync_queue";

    // Column names
    private static final String COL_ID = "id";
    private static final String COL_OPERATION_TYPE = "operation_type"; // CREATE, UPDATE, DELETE
    private static final String COL_ENTITY_TYPE = "entity_type"; // USER, EXPENSE, MEAL, MESS
    private static final String COL_ENTITY_ID = "entity_id"; // Local entity ID
    private static final String COL_FIREBASE_ID = "firebase_id"; // Firebase document ID (if exists)
    private static final String COL_FIREBASE_MESS_ID = "firebase_mess_id"; // Firebase mess ID
    private static final String COL_DATA_JSON = "data_json"; // Serialized entity data
    private static final String COL_TIMESTAMP = "timestamp"; // When the change was made
    private static final String COL_RETRY_COUNT = "retry_count"; // Number of sync attempts
    private static final String COL_LAST_ERROR = "last_error"; // Last error message
    private static final String COL_STATUS = "status"; // PENDING, PROCESSING, FAILED

    // Operation types
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";

    // Entity types
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_EXPENSE = "EXPENSE";
    public static final String ENTITY_MEAL = "MEAL";
    public static final String ENTITY_MESS = "MESS";

    // Status values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_FAILED = "FAILED";

    // Max retry count
    private static final int MAX_RETRY_COUNT = 5;

    // Singleton instance
    private static OfflineQueueManager instance;
    private final Context context;
    private final Gson gson;
    private final ExecutorService executor;

    // Callback for queue processing
    public interface QueueProcessCallback {
        void onQueueProcessed(int successCount, int failureCount);

        void onQueueEmpty();
    }

    private OfflineQueueManager(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized OfflineQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineQueueManager(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_QUEUE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_OPERATION_TYPE + " TEXT NOT NULL, " +
                COL_ENTITY_TYPE + " TEXT NOT NULL, " +
                COL_ENTITY_ID + " TEXT, " +
                COL_FIREBASE_ID + " TEXT, " +
                COL_FIREBASE_MESS_ID + " TEXT, " +
                COL_DATA_JSON + " TEXT NOT NULL, " +
                COL_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_RETRY_COUNT + " INTEGER DEFAULT 0, " +
                COL_LAST_ERROR + " TEXT, " +
                COL_STATUS + " TEXT DEFAULT 'PENDING'" +
                ")";
        db.execSQL(createTable);

        // Create index for faster queries
        db.execSQL("CREATE INDEX idx_status ON " + TABLE_QUEUE + " (" + COL_STATUS + ")");
        db.execSQL("CREATE INDEX idx_entity ON " + TABLE_QUEUE + " (" + COL_ENTITY_TYPE + ", " + COL_ENTITY_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUEUE);
        onCreate(db);
    }

    /**
     * Queue item data class
     */
    public static class QueueItem {
        public long id;
        public String operationType;
        public String entityType;
        public String entityId;
        public String firebaseId;
        public String firebaseMessId;
        public String dataJson;
        public long timestamp;
        public int retryCount;
        public String lastError;
        public String status;

        public QueueItem() {
        }
    }

    /**
     * Add an operation to the queue
     */
    public long queueOperation(String operationType, String entityType, String entityId,
            String firebaseId, String firebaseMessId, Object data) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_OPERATION_TYPE, operationType);
        values.put(COL_ENTITY_TYPE, entityType);
        values.put(COL_ENTITY_ID, entityId);
        values.put(COL_FIREBASE_ID, firebaseId);
        values.put(COL_FIREBASE_MESS_ID, firebaseMessId);
        values.put(COL_DATA_JSON, gson.toJson(data));
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_STATUS, STATUS_PENDING);

        long id = db.insert(TABLE_QUEUE, null, values);
        Log.d(TAG, "Queued operation: " + operationType + " " + entityType + " ID=" + entityId);

        // Try to process immediately if online
        if (isNetworkAvailable()) {
            processQueue(null);
        }

        return id;
    }

    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null)
            return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Check if there are pending operations in the queue
     */
    public boolean hasPendingOperations() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_QUEUE +
                        " WHERE " + COL_STATUS + " IN (?, ?)",
                new String[] { STATUS_PENDING, STATUS_FAILED });

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count > 0;
    }

    /**
     * Get count of pending operations
     */
    public int getPendingCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_QUEUE +
                        " WHERE " + COL_STATUS + " IN (?, ?)",
                new String[] { STATUS_PENDING, STATUS_FAILED });

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Get all pending queue items
     */
    public List<QueueItem> getPendingItems() {
        List<QueueItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_QUEUE +
                        " WHERE " + COL_STATUS + " IN (?, ?) AND " + COL_RETRY_COUNT + " < ?" +
                        " ORDER BY " + COL_TIMESTAMP + " ASC",
                new String[] { STATUS_PENDING, STATUS_FAILED, String.valueOf(MAX_RETRY_COUNT) });

        while (cursor.moveToNext()) {
            QueueItem item = new QueueItem();
            item.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            item.operationType = cursor.getString(cursor.getColumnIndexOrThrow(COL_OPERATION_TYPE));
            item.entityType = cursor.getString(cursor.getColumnIndexOrThrow(COL_ENTITY_TYPE));
            item.entityId = cursor.getString(cursor.getColumnIndexOrThrow(COL_ENTITY_ID));
            item.firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_ID));
            item.firebaseMessId = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_MESS_ID));
            item.dataJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA_JSON));
            item.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
            item.retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RETRY_COUNT));
            item.lastError = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_ERROR));
            item.status = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS));
            items.add(item);
        }
        cursor.close();

        return items;
    }

    /**
     * Update item status
     */
    private void updateItemStatus(long itemId, String status, String error) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        if (error != null) {
            values.put(COL_LAST_ERROR, error);
        }
        db.update(TABLE_QUEUE, values, COL_ID + " = ?", new String[] { String.valueOf(itemId) });
    }

    /**
     * Increment retry count
     */
    private void incrementRetryCount(long itemId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_QUEUE + " SET " + COL_RETRY_COUNT + " = " +
                COL_RETRY_COUNT + " + 1 WHERE " + COL_ID + " = ?",
                new Object[] { itemId });
    }

    /**
     * Remove item from queue (after successful sync)
     */
    private void removeItem(long itemId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_QUEUE, COL_ID + " = ?", new String[] { String.valueOf(itemId) });
        Log.d(TAG, "Removed queue item: " + itemId);
    }

    /**
     * Clear all completed/successful items
     */
    public void clearCompletedItems() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_QUEUE, COL_STATUS + " = ?", new String[] { STATUS_PROCESSING });
    }

    /**
     * Remove duplicate operations for the same entity
     * Keeps only the latest operation for each entity
     */
    public void deduplicateQueue() {
        SQLiteDatabase db = getWritableDatabase();

        // Delete older operations for the same entity, keeping only the latest
        db.execSQL(
                "DELETE FROM " + TABLE_QUEUE + " WHERE " + COL_ID + " NOT IN (" +
                        "SELECT MAX(" + COL_ID + ") FROM " + TABLE_QUEUE +
                        " GROUP BY " + COL_ENTITY_TYPE + ", " + COL_ENTITY_ID + ")");
    }

    /**
     * Process all pending items in the queue
     */
    public void processQueue(QueueProcessCallback callback) {
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                Log.d(TAG, "No network available, skipping queue processing");
                if (callback != null) {
                    callback.onQueueEmpty();
                }
                return;
            }

            List<QueueItem> pendingItems = getPendingItems();

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "Queue is empty");
                if (callback != null) {
                    callback.onQueueEmpty();
                }
                return;
            }

            Log.d(TAG, "Processing " + pendingItems.size() + " queued items");

            SyncManager syncManager = SyncManager.getInstance(context);
            FirebaseRepository firebaseRepo = FirebaseRepository.getInstance();

            int successCount = 0;
            int failureCount = 0;

            for (QueueItem item : pendingItems) {
                // Mark as processing
                updateItemStatus(item.id, STATUS_PROCESSING, null);

                try {
                    boolean success = processQueueItem(item, syncManager, firebaseRepo);

                    if (success) {
                        removeItem(item.id);
                        successCount++;
                    } else {
                        incrementRetryCount(item.id);
                        updateItemStatus(item.id, STATUS_FAILED, "Sync failed");
                        failureCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing queue item: " + item.id, e);
                    incrementRetryCount(item.id);
                    updateItemStatus(item.id, STATUS_FAILED, e.getMessage());
                    failureCount++;
                }
            }

            Log.d(TAG, "Queue processed: " + successCount + " success, " + failureCount + " failed");

            if (callback != null) {
                int finalSuccessCount = successCount;
                int finalFailureCount = failureCount;
                callback.onQueueProcessed(finalSuccessCount, finalFailureCount);
            }
        });
    }

    /**
     * Process a single queue item
     */
    private boolean processQueueItem(QueueItem item, SyncManager syncManager,
            FirebaseRepository firebaseRepo) {
        try {
            switch (item.entityType) {
                case ENTITY_EXPENSE:
                    return processExpenseOperation(item, firebaseRepo);

                case ENTITY_MEAL:
                    return processMealOperation(item, firebaseRepo);

                case ENTITY_USER:
                    return processUserOperation(item, firebaseRepo);

                case ENTITY_MESS:
                    return processMessOperation(item, firebaseRepo);

                default:
                    Log.w(TAG, "Unknown entity type: " + item.entityType);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing " + item.entityType + " operation", e);
            return false;
        }
    }

    private boolean processExpenseOperation(QueueItem item, FirebaseRepository firebaseRepo) {
        SyncableExpense expense = gson.fromJson(item.dataJson, SyncableExpense.class);

        switch (item.operationType) {
            case OP_CREATE:
            case OP_UPDATE:
                // Sync expense to cloud
                firebaseRepo.saveExpense(expense);
                return true;

            case OP_DELETE:
                if (item.firebaseId != null) {
                    firebaseRepo.deleteExpense(item.firebaseId);
                }
                return true;

            default:
                return false;
        }
    }

    private boolean processMealOperation(QueueItem item, FirebaseRepository firebaseRepo) {
        SyncableMeal meal = gson.fromJson(item.dataJson, SyncableMeal.class);

        switch (item.operationType) {
            case OP_CREATE:
            case OP_UPDATE:
                firebaseRepo.saveMeal(meal);
                return true;

            case OP_DELETE:
                if (item.firebaseId != null) {
                    firebaseRepo.deleteMeal(item.firebaseId);
                }
                return true;

            default:
                return false;
        }
    }

    private boolean processUserOperation(QueueItem item, FirebaseRepository firebaseRepo) {
        SyncableUser user = gson.fromJson(item.dataJson, SyncableUser.class);

        switch (item.operationType) {
            case OP_CREATE:
            case OP_UPDATE:
                firebaseRepo.saveUser(user);
                return true;

            default:
                return false;
        }
    }

    private boolean processMessOperation(QueueItem item, FirebaseRepository firebaseRepo) {
        SyncableMess mess = gson.fromJson(item.dataJson, SyncableMess.class);

        switch (item.operationType) {
            case OP_CREATE:
            case OP_UPDATE:
                firebaseRepo.saveMess(mess);
                return true;

            default:
                return false;
        }
    }
}
