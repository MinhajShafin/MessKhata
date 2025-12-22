package com.messkhata.data.sync.model;

import com.messkhata.data.model.Meal;
import com.messkhata.data.sync.SyncStatus;
import com.messkhata.data.sync.SyncableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncable wrapper for Meal entity
 */
public class SyncableMeal extends Meal implements SyncableEntity {

    public static final String COLLECTION_NAME = "meals";

    private String firebaseId;
    private SyncStatus syncStatus = SyncStatus.PENDING_UPLOAD;
    private long lastModified;

    public SyncableMeal() {
        super();
    }

    public SyncableMeal(Meal meal) {
        super(meal.getMealId(), meal.getUserId(), meal.getMessId(),
                meal.getMealDate(), meal.getBreakfast(), meal.getLunch(),
                meal.getDinner());
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public long getLocalId() {
        return getMealId();
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
        map.put("mealId", getMealId());
        map.put("userId", getUserId());
        map.put("messId", getMessId());
        map.put("mealDate", getMealDate());
        map.put("breakfast", getBreakfast());
        map.put("lunch", getLunch());
        map.put("dinner", getDinner());
        map.put("totalMeals", getTotalMeals());
        map.put("lastModified", lastModified);
        return map;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION_NAME;
    }

    /**
     * Create SyncableMeal from Firebase document
     */
    public static SyncableMeal fromFirebaseMap(String documentId, Map<String, Object> data) {
        SyncableMeal meal = new SyncableMeal();
        meal.setFirebaseId(documentId);

        if (data.containsKey("mealId")) {
            meal.setMealId(((Number) data.get("mealId")).intValue());
        }
        if (data.containsKey("userId")) {
            meal.setUserId(((Number) data.get("userId")).intValue());
        }
        if (data.containsKey("messId")) {
            meal.setMessId(((Number) data.get("messId")).intValue());
        }
        if (data.containsKey("mealDate")) {
            meal.setMealDate(((Number) data.get("mealDate")).longValue());
        }
        if (data.containsKey("breakfast")) {
            meal.setBreakfast(((Number) data.get("breakfast")).intValue());
        }
        if (data.containsKey("lunch")) {
            meal.setLunch(((Number) data.get("lunch")).intValue());
        }
        if (data.containsKey("dinner")) {
            meal.setDinner(((Number) data.get("dinner")).intValue());
        }
        if (data.containsKey("lastModified")) {
            meal.setLastModified(((Number) data.get("lastModified")).longValue());
        }

        meal.setSyncStatus(SyncStatus.SYNCED);
        return meal;
    }
}
