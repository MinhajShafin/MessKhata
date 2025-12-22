package com.messkhata.data.sync.model;

import com.messkhata.data.model.Mess;
import com.messkhata.data.sync.SyncStatus;
import com.messkhata.data.sync.SyncableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncable wrapper for Mess entity
 */
public class SyncableMess extends Mess implements SyncableEntity {

    public static final String COLLECTION_NAME = "messes";

    private String firebaseId;
    private SyncStatus syncStatus = SyncStatus.PENDING_UPLOAD;
    private long lastModified;

    public SyncableMess() {
        super();
    }

    public SyncableMess(Mess mess) {
        super(mess.getMessId(), mess.getMessName(),
                mess.getGroceryBudgetPerMeal(), mess.getCookingChargePerMeal(),
                mess.getCreatedDate());
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public long getLocalId() {
        return getMessId();
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
        map.put("messId", getMessId());
        map.put("messName", getMessName());
        map.put("groceryBudgetPerMeal", getGroceryBudgetPerMeal());
        map.put("cookingChargePerMeal", getCookingChargePerMeal());
        map.put("createdDate", getCreatedDate());
        map.put("lastModified", lastModified);
        return map;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION_NAME;
    }

    /**
     * Create SyncableMess from Firebase document
     */
    public static SyncableMess fromFirebaseMap(String documentId, Map<String, Object> data) {
        SyncableMess mess = new SyncableMess();
        mess.setFirebaseId(documentId);

        if (data.containsKey("messId")) {
            mess.setMessId(((Number) data.get("messId")).intValue());
        }
        if (data.containsKey("messName")) {
            mess.setMessName((String) data.get("messName"));
        }
        if (data.containsKey("groceryBudgetPerMeal")) {
            mess.setGroceryBudgetPerMeal(((Number) data.get("groceryBudgetPerMeal")).doubleValue());
        }
        if (data.containsKey("cookingChargePerMeal")) {
            mess.setCookingChargePerMeal(((Number) data.get("cookingChargePerMeal")).doubleValue());
        }
        if (data.containsKey("createdDate")) {
            mess.setCreatedDate(((Number) data.get("createdDate")).longValue());
        }
        if (data.containsKey("lastModified")) {
            mess.setLastModified(((Number) data.get("lastModified")).longValue());
        }

        mess.setSyncStatus(SyncStatus.SYNCED);
        return mess;
    }
}
