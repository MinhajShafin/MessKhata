package com.messkhata.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.messkhata.data.dao.NotificationDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Notification;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.utils.IdGenerator;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * Repository for Notification operations.
 */
public class NotificationRepository {

    private final NotificationDao notificationDao;
    private final FirebaseSyncManager syncManager;
    private final NetworkUtils networkUtils;
    private final PreferenceManager prefManager;

    public NotificationRepository(Application application) {
        MessKhataDatabase database = MessKhataDatabase.getInstance(application);
        notificationDao = database.notificationDao();
        syncManager = FirebaseSyncManager.getInstance(database);
        networkUtils = NetworkUtils.getInstance(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get all notifications for current user.
     */
    public LiveData<List<Notification>> getAllNotifications() {
        String userId = prefManager.getUserId();
        return notificationDao.getNotificationsByUser(userId);
    }

    /**
     * Get unread notifications.
     */
    public LiveData<List<Notification>> getUnreadNotifications() {
        String userId = prefManager.getUserId();
        return notificationDao.getUnreadNotifications(userId);
    }

    /**
     * Get unread count.
     */
    public LiveData<Integer> getUnreadCount() {
        String userId = prefManager.getUserId();
        return notificationDao.getUnreadCount(userId);
    }

    /**
     * Mark notification as read.
     */
    public void markAsRead(String notificationId) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.markAsRead(notificationId);
        });
    }

    /**
     * Mark all notifications as read.
     */
    public void markAllAsRead() {
        String userId = prefManager.getUserId();
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.markAllAsRead(userId);
        });
    }

    /**
     * Create a local notification.
     */
    public void createNotification(String type, String title, String message,
                                    String actionData) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String userId = prefManager.getUserId();
            String messId = prefManager.getMessId();
            String notificationId = IdGenerator.generateId();

            Notification notification = new Notification(notificationId, userId, messId,
                                                        type, title, message);
            notification.setActionData(actionData);

            notificationDao.insert(notification);
        });
    }

    /**
     * Delete old notifications (older than 30 days).
     */
    public void cleanupOldNotifications() {
        String userId = prefManager.getUserId();
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            notificationDao.deleteOldNotifications(userId, thirtyDaysAgo);
        });
    }
}
