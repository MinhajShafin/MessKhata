package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Notification entity for storing app notifications.
 * Types: PAYMENT_REMINDER, MEAL_ENTRY_REMINDER, NEW_EXPENSE, REPORT_READY, GENERAL
 */
@Entity(tableName = "notifications",
        indices = {
            @Index(value = {"userId"}),
            @Index(value = {"isRead"})
        })
public class Notification {

    @PrimaryKey
    @NonNull
    private String id;

    private String userId;
    private String messId;
    private String type; // PAYMENT_REMINDER, MEAL_ENTRY_REMINDER, NEW_EXPENSE, REPORT_READY, GENERAL
    private String title;
    private String message;
    private String actionData; // JSON data for action (e.g., reportId, expenseId)
    private boolean isRead;
    private long createdAt;
    private boolean isSynced;
    private String pendingAction;

    public Notification() {
        this.id = "";
    }

    public Notification(@NonNull String id, String userId, String messId,
                        String type, String title, String message) {
        this.id = id;
        this.userId = userId;
        this.messId = messId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
        this.isSynced = false;
        this.pendingAction = "CREATE";
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessId() { return messId; }
    public void setMessId(String messId) { this.messId = messId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionData() { return actionData; }
    public void setActionData(String actionData) { this.actionData = actionData; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }

    // Notification type constants
    public static final String PAYMENT_REMINDER = "PAYMENT_REMINDER";
    public static final String MEAL_ENTRY_REMINDER = "MEAL_ENTRY_REMINDER";
    public static final String NEW_EXPENSE = "NEW_EXPENSE";
    public static final String REPORT_READY = "REPORT_READY";
    public static final String GENERAL = "GENERAL";
}
