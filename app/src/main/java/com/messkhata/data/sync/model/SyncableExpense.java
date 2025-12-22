package com.messkhata.data.sync.model;

import com.messkhata.data.model.Expense;
import com.messkhata.data.sync.SyncStatus;
import com.messkhata.data.sync.SyncableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncable wrapper for Expense entity
 */
public class SyncableExpense extends Expense implements SyncableEntity {

    public static final String COLLECTION_NAME = "expenses";

    private String firebaseId;
    private SyncStatus syncStatus = SyncStatus.PENDING_UPLOAD;
    private long lastModified;

    public SyncableExpense() {
        super();
    }

    public SyncableExpense(Expense expense) {
        super(expense.getExpenseId(), expense.getMessId(), expense.getAddedBy(),
                expense.getCategory(), expense.getAmount(), expense.getTitle(),
                expense.getDescription(), expense.getExpenseDate(), expense.getCreatedAt());
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public long getLocalId() {
        return getExpenseId();
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
        map.put("expenseId", getExpenseId());
        map.put("messId", getMessId());
        map.put("addedBy", getAddedBy());
        map.put("category", getCategory());
        map.put("amount", getAmount());
        map.put("title", getTitle());
        map.put("description", getDescription());
        map.put("expenseDate", getExpenseDate());
        map.put("createdAt", getCreatedAt());
        map.put("lastModified", lastModified);
        return map;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION_NAME;
    }

    /**
     * Create SyncableExpense from Firebase document
     */
    public static SyncableExpense fromFirebaseMap(String documentId, Map<String, Object> data) {
        SyncableExpense expense = new SyncableExpense();
        expense.setFirebaseId(documentId);

        if (data.containsKey("expenseId")) {
            expense.setExpenseId(((Number) data.get("expenseId")).intValue());
        }
        if (data.containsKey("messId")) {
            expense.setMessId(((Number) data.get("messId")).intValue());
        }
        if (data.containsKey("addedBy")) {
            expense.setAddedBy(((Number) data.get("addedBy")).intValue());
        }
        if (data.containsKey("category")) {
            expense.setCategory((String) data.get("category"));
        }
        if (data.containsKey("amount")) {
            expense.setAmount(((Number) data.get("amount")).doubleValue());
        }
        if (data.containsKey("title")) {
            expense.setTitle((String) data.get("title"));
        }
        if (data.containsKey("description")) {
            expense.setDescription((String) data.get("description"));
        }
        if (data.containsKey("expenseDate")) {
            expense.setExpenseDate(((Number) data.get("expenseDate")).longValue());
        }
        if (data.containsKey("createdAt")) {
            expense.setCreatedAt(((Number) data.get("createdAt")).longValue());
        }
        if (data.containsKey("lastModified")) {
            expense.setLastModified(((Number) data.get("lastModified")).longValue());
        }

        expense.setSyncStatus(SyncStatus.SYNCED);
        return expense;
    }
}
