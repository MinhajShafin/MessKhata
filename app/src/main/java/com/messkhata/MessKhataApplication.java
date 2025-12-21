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
    }

    public static MessKhataApplication getInstance() {
        return instance;
    }


    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}
