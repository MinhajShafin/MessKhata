package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Meal entity representing a single meal entry for a user on a specific date.
 * Meal types: BREAKFAST, LUNCH, DINNER
 */
@Entity(tableName = "meals",
        indices = {
            @Index(value = {"userId", "date", "mealType"}, unique = true),
            @Index(value = {"messId"}),
            @Index(value = {"date"})
        })
public class Meal {

    @PrimaryKey
    @NonNull
    private String id;

    private String messId;
    private String userId;
    private String userName; // Denormalized for quick display
    private long date; // Date in milliseconds (start of day)
    private String mealType; // BREAKFAST, LUNCH, DINNER
    private int count; // Number of meals (0, 1, 2, etc. for guests)
    private String notes;
    private long createdAt;
    private long updatedAt;
    private String createdBy;
    private boolean isSynced;
    private String pendingAction;

    public Meal() {
        this.id = "";
    }

    public Meal(@NonNull String id, String messId, String userId, String userName,
                long date, String mealType, int count) {
        this.id = id;
        this.messId = messId;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.mealType = mealType;
        this.count = count;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.pendingAction = "CREATE";
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getMessId() { return messId; }
    public void setMessId(String messId) { this.messId = messId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }

    // Meal type constants
    public static final String BREAKFAST = "BREAKFAST";
    public static final String LUNCH = "LUNCH";
    public static final String DINNER = "DINNER";
}
