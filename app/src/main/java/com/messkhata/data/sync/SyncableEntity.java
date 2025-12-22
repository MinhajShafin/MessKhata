package com.messkhata.data.sync;

import java.util.Map;

/**
 * Interface for entities that can be synced with Firebase
 */
public interface SyncableEntity {

    /**
     * Get the local ID of this entity
     */
    long getLocalId();

    /**
     * Get the Firebase document ID
     */
    String getFirebaseId();

    /**
     * Set the Firebase document ID
     */
    void setFirebaseId(String firebaseId);

    /**
     * Get the sync status
     */
    SyncStatus getSyncStatus();

    /**
     * Set the sync status
     */
    void setSyncStatus(SyncStatus status);

    /**
     * Get last modified timestamp
     */
    long getLastModified();

    /**
     * Set last modified timestamp
     */
    void setLastModified(long timestamp);

    /**
     * Convert entity to Firebase document map
     */
    Map<String, Object> toFirebaseMap();

    /**
     * Get the Firestore collection name for this entity type
     */
    String getCollectionName();
}
