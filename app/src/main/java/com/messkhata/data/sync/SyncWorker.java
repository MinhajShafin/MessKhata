package com.messkhata.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.messkhata.utils.PreferenceManager;

import java.util.concurrent.TimeUnit;

/**
 * Background worker for periodic data synchronization with Firebase
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    public static final String WORK_NAME = "MessKhataSyncWork";

    // Sync interval in hours
    private static final int SYNC_INTERVAL_HOURS = 1;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync...");

        try {
            Context context = getApplicationContext();
            SyncManager syncManager = SyncManager.getInstance(context);

            // Check if sync is enabled and user is authenticated
            if (!syncManager.isSyncEnabled()) {
                Log.d(TAG, "Sync is disabled, skipping");
                return Result.success();
            }

            if (!syncManager.isAuthenticated()) {
                Log.d(TAG, "User not authenticated, skipping sync");
                return Result.success();
            }

            // Get current user's mess ID from preferences
            PreferenceManager prefs = PreferenceManager.getInstance(context);
            String messIdStr = prefs.getMessId();

            if (messIdStr == null || messIdStr.isEmpty()) {
                Log.d(TAG, "No mess ID found, skipping sync");
                return Result.success();
            }

            int messId;
            try {
                messId = Integer.parseInt(messIdStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid mess ID format", e);
                return Result.success();
            }

            // Perform sync (blocking call in worker)
            performSyncBlocking(syncManager, messId);

            Log.d(TAG, "Background sync completed successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Background sync failed", e);
            return Result.retry();
        }
    }

    /**
     * Perform sync in blocking mode for WorkManager
     */
    private void performSyncBlocking(SyncManager syncManager, int messId) {
        // Create a blocking sync callback
        final Object lock = new Object();
        final boolean[] completed = { false };
        final String[] error = { null };

        syncManager.setSyncCallback(new SyncCallback() {
            @Override
            public void onSyncStarted() {
                Log.d(TAG, "Background sync started");
            }

            @Override
            public void onSyncCompleted() {
                synchronized (lock) {
                    completed[0] = true;
                    lock.notify();
                }
            }

            @Override
            public void onSyncFailed(String errorMsg) {
                synchronized (lock) {
                    error[0] = errorMsg;
                    completed[0] = true;
                    lock.notify();
                }
            }

            @Override
            public void onSyncProgress(int progress, String message) {
                Log.d(TAG, "Sync progress: " + progress + "% - " + message);
            }
        });

        // Start sync
        syncManager.performFullSync(messId);

        // Wait for completion (with timeout)
        synchronized (lock) {
            try {
                if (!completed[0]) {
                    lock.wait(5 * 60 * 1000); // 5 minute timeout
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (error[0] != null) {
            throw new RuntimeException("Sync failed: " + error[0]);
        }
    }

    /**
     * Schedule periodic sync work
     */
    public static void schedulePeriodicSync(Context context) {
        // Define constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        // Create periodic work request
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.MINUTES) // Delay first run
                .build();

        // Enqueue unique periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest);

        Log.d(TAG, "Periodic sync scheduled every " + SYNC_INTERVAL_HOURS + " hours");
    }

    /**
     * Cancel scheduled sync work
     */
    public static void cancelPeriodicSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Periodic sync cancelled");
    }

    /**
     * Trigger immediate sync using WorkManager
     */
    public static void triggerImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
        Log.d(TAG, "Immediate sync triggered");
    }
}
