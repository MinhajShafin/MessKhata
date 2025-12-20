package com.messkhata;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.utils.Constants;
import com.messkhata.utils.PreferenceManager;

public class MessKhataApplication extends Application implements Configuration.Provider {

    private static MessKhataApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        MessKhataDatabase.getInstance(this).getWritableDatabase();

        // Create notification channels
        createNotificationChannels();
    }

    public static MessKhataApplication getInstance() {
        return instance;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // General notifications channel
            NotificationChannel generalChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(generalChannel);

            // Reminders channel
            NotificationChannel remindersChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_REMINDERS,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            remindersChannel.setDescription("Daily meal entry reminders");
            notificationManager.createNotificationChannel(remindersChannel);

            // Sync channel
            NotificationChannel syncChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_SYNC,
                    "Sync Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            syncChannel.setDescription("Background sync status");
            notificationManager.createNotificationChannel(syncChannel);
        }
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}
