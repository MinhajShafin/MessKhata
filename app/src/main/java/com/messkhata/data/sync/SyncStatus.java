package com.messkhata.data.sync;

/**
 * Enum representing the sync status of a record
 */
public enum SyncStatus {
    SYNCED("synced"), // Record is synced with cloud
    PENDING_UPLOAD("pending"), // Record needs to be uploaded
    PENDING_DELETE("delete"), // Record needs to be deleted from cloud
    CONFLICT("conflict"); // Sync conflict detected

    private final String value;

    SyncStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SyncStatus fromString(String value) {
        for (SyncStatus status : SyncStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return PENDING_UPLOAD;
    }
}
