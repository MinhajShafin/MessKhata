package com.messkhata;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.sync.FirebaseRepository;
import com.messkhata.data.sync.SyncWorker;
import com.messkhata.utils.Constants;
import com.messkhata.utils.PreferenceManager;

public class MessKhataApplication extends Application implements Configuration.Provider {

    private static final String TAG = "MessKhataApplication";
    private static MessKhataApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize local database
        MessKhataDatabase.getInstance(this).getWritableDatabase();

        // Initialize Firebase
        initializeFirebase();

        // Initialize PreferenceManager
        PreferenceManager.init(this);

        // Schedule periodic sync
        scheduleSyncWork();

        Log.d(TAG, "MessKhata Application initialized");
    }

    public static MessKhataApplication getInstance() {
        return instance;
    }

    /**
     * Initialize Firebase services
     */
    private void initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this);

            // Enable Firestore offline persistence
            FirebaseRepository.getInstance().enableOfflinePersistence();

            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }

    /**
     * Schedule background sync work
     */
    private void scheduleSyncWork() {
        try {
            SyncWorker.schedulePeriodicSync(this);
            Log.d(TAG, "Sync work scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule sync work", e);
        }
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}
