package com.messkhata.data.sync;

/**
 * Callback interface for sync operations
 */
public interface SyncCallback {

    /**
     * Called when sync starts
     */
    void onSyncStarted();

    /**
     * Called when sync completes successfully
     */
    void onSyncCompleted();

    /**
     * Called when sync fails
     * 
     * @param error Error message
     */
    void onSyncFailed(String error);

    /**
     * Called to report sync progress
     * 
     * @param progress Progress percentage (0-100)
     * @param message  Status message
     */
    void onSyncProgress(int progress, String message);
}
