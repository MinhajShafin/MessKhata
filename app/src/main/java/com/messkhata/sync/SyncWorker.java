package com.messkhata.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.utils.Constants;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.concurrent.TimeUnit;

/**
 * Background sync worker using WorkManager.
 * Handles periodic synchronization and uploads pending changes when online.
 */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PreferenceManager prefManager = PreferenceManager.getInstance(context);

        // Check if user is logged in
        if (!prefManager.isLoggedIn()) {
            return Result.success();
        }

        String messId = prefManager.getMessId();
        String userId = prefManager.getUserId();

        if (messId == null || userId == null) {
            return Result.success();
        }

        // Check network availability
        NetworkUtils networkUtils = NetworkUtils.getInstance(context);
        if (!networkUtils.isOnline()) {
            return Result.retry(); // Retry when network is available
        }

        try {
            // Get database and sync manager
            MessKhataDatabase database = MessKhataDatabase.getInstance(context);
            FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(database);

            // Upload pending changes
            syncManager.uploadPendingChanges();

            // Update last sync time
            prefManager.setLastSyncTime(System.currentTimeMillis());

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * Schedule periodic sync work.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15, TimeUnit.MINUTES  // Minimum interval
        )
                .setConstraints(constraints)
                .addTag(Constants.WORK_SYNC)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        Constants.WORK_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWork
                );
    }

    /**
     * Trigger immediate sync.
     */
    public static void triggerImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(Constants.WORK_SYNC)
                .build();

        WorkManager.getInstance(context).enqueue(syncWork);
    }

    /**
     * Cancel all sync work.
     */
    public static void cancelSync(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(Constants.WORK_SYNC);
    }
}
