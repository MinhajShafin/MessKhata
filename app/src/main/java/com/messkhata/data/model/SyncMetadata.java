package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * SyncMetadata entity for tracking synchronization state.
 * Used to manage offline-first sync operations.
 */
@Entity(tableName = "sync_metadata")
public class SyncMetadata {

    @PrimaryKey
    @NonNull
    private String tableName;

    private long lastSyncTime;
    private int pendingOperations;
    private String lastError;
    private boolean isSyncing;

    public SyncMetadata() {
        this.tableName = "";
    }

    public SyncMetadata(@NonNull String tableName) {
        this.tableName = tableName;
        this.lastSyncTime = 0;
        this.pendingOperations = 0;
        this.isSyncing = false;
    }

    // Getters and Setters
    @NonNull
    public String getTableName() { return tableName; }
    public void setTableName(@NonNull String tableName) { this.tableName = tableName; }

    public long getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    public int getPendingOperations() { return pendingOperations; }
    public void setPendingOperations(int pendingOperations) {
        this.pendingOperations = pendingOperations;
    }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public boolean isSyncing() { return isSyncing; }
    public void setSyncing(boolean syncing) { isSyncing = syncing; }
}
