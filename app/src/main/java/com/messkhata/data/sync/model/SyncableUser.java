package com.messkhata.data.sync.model;

import com.messkhata.data.model.User;
import com.messkhata.data.sync.SyncStatus;
import com.messkhata.data.sync.SyncableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncable wrapper for User entity
 */
public class SyncableUser extends User implements SyncableEntity {

    public static final String COLLECTION_NAME = "users";

    private String firebaseId;
    private SyncStatus syncStatus = SyncStatus.PENDING_UPLOAD;
    private long lastModified;

    public SyncableUser() {
        super();
    }

    public SyncableUser(User user) {
        super(user.getUserId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getMessId(), user.getRole(),
                user.getJoinedDate());
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public long getLocalId() {
        return getUserId();
    }

    @Override
    public String getFirebaseId() {
        return firebaseId;
    }

    @Override
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    @Override
    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    @Override
    public void setSyncStatus(SyncStatus status) {
        this.syncStatus = status;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long timestamp) {
        this.lastModified = timestamp;
    }

    @Override
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", getUserId());
        map.put("fullName", getFullName());
        map.put("email", getEmail());
        map.put("phoneNumber", getPhoneNumber());
        map.put("messId", getMessId());
        map.put("role", getRole());
        map.put("joinedDate", getJoinedDate());
        map.put("lastModified", lastModified);
        return map;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION_NAME;
    }

    /**
     * Create SyncableUser from Firebase document
     */
    public static SyncableUser fromFirebaseMap(String documentId, Map<String, Object> data) {
        SyncableUser user = new SyncableUser();
        user.setFirebaseId(documentId);

        if (data.containsKey("userId")) {
            user.setUserId(((Number) data.get("userId")).longValue());
        }
        if (data.containsKey("fullName")) {
            user.setFullName((String) data.get("fullName"));
        }
        if (data.containsKey("email")) {
            user.setEmail((String) data.get("email"));
        }
        if (data.containsKey("phoneNumber")) {
            user.setPhoneNumber((String) data.get("phoneNumber"));
        }
        if (data.containsKey("messId")) {
            user.setMessId(((Number) data.get("messId")).intValue());
        }
        if (data.containsKey("role")) {
            user.setRole((String) data.get("role"));
        }
        if (data.containsKey("joinedDate")) {
            user.setJoinedDate(((Number) data.get("joinedDate")).longValue());
        }
        if (data.containsKey("lastModified")) {
            user.setLastModified(((Number) data.get("lastModified")).longValue());
        }

        user.setSyncStatus(SyncStatus.SYNCED);
        return user;
    }
}
