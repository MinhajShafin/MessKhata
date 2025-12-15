package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.Notification;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Notification notification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Notification> notifications);

    @Update
    void update(Notification notification);

    @Delete
    void delete(Notification notification);

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    Notification getNotificationById(String notificationId);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Notification>> getNotificationsByUser(String userId);

    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    LiveData<List<Notification>> getUnreadNotifications(String userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    LiveData<Integer> getUnreadCount(String userId);

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(String notificationId);

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(String userId);

    @Query("SELECT * FROM notifications WHERE isSynced = 0")
    List<Notification> getUnsyncedNotifications();

    @Query("UPDATE notifications SET isSynced = 1, pendingAction = NULL WHERE id = :notificationId")
    void markAsSynced(String notificationId);

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    void deleteById(String notificationId);

    @Query("DELETE FROM notifications WHERE userId = :userId AND createdAt < :timestamp")
    void deleteOldNotifications(String userId, long timestamp);
}
