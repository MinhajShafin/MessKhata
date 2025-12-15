package com.messkhata.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.SyncMetadata;

import java.util.List;

@Dao
public interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncMetadata metadata);

    @Update
    void update(SyncMetadata metadata);

    @Query("SELECT * FROM sync_metadata WHERE tableName = :tableName")
    SyncMetadata getMetadata(String tableName);

    @Query("SELECT * FROM sync_metadata")
    List<SyncMetadata> getAllMetadata();

    @Query("UPDATE sync_metadata SET lastSyncTime = :timestamp WHERE tableName = :tableName")
    void updateLastSyncTime(String tableName, long timestamp);

    @Query("UPDATE sync_metadata SET pendingOperations = pendingOperations + 1 WHERE tableName = :tableName")
    void incrementPendingOperations(String tableName);

    @Query("UPDATE sync_metadata SET pendingOperations = pendingOperations - 1 WHERE tableName = :tableName AND pendingOperations > 0")
    void decrementPendingOperations(String tableName);

    @Query("UPDATE sync_metadata SET pendingOperations = 0 WHERE tableName = :tableName")
    void resetPendingOperations(String tableName);

    @Query("UPDATE sync_metadata SET isSyncing = :isSyncing WHERE tableName = :tableName")
    void setSyncing(String tableName, boolean isSyncing);

    @Query("UPDATE sync_metadata SET lastError = :error WHERE tableName = :tableName")
    void setLastError(String tableName, String error);

    @Query("SELECT SUM(pendingOperations) FROM sync_metadata")
    int getTotalPendingOperations();
}
