package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.User;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(String userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(String userId);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE messId = :messId AND isActive = 1")
    LiveData<List<User>> getActiveUsersByMess(String messId);

    @Query("SELECT * FROM users WHERE messId = :messId AND isActive = 1")
    List<User> getActiveUsersByMessSync(String messId);

    @Query("SELECT * FROM users WHERE messId = :messId")
    LiveData<List<User>> getAllUsersByMess(String messId);

    @Query("SELECT COUNT(*) FROM users WHERE messId = :messId AND isActive = 1")
    int getActiveMemberCount(String messId);

    @Query("SELECT * FROM users WHERE isSynced = 0")
    List<User> getUnsyncedUsers();

    @Query("UPDATE users SET isSynced = 1, pendingAction = NULL WHERE id = :userId")
    void markAsSynced(String userId);

    @Query("UPDATE users SET pendingAction = :action, isSynced = 0, updatedAt = :timestamp WHERE id = :userId")
    void markForSync(String userId, String action, long timestamp);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(String userId);
}
