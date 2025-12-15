package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Expense entity representing shared expenses like groceries, utilities, etc.
 * Categories: GROCERY, UTILITY, GAS, RENT, MAINTENANCE, OTHER
 */
@Entity(tableName = "expenses",
        indices = {
            @Index(value = {"messId"}),
            @Index(value = {"date"}),
            @Index(value = {"category"})
        })
public class Expense {

    @PrimaryKey
    @NonNull
    private String id;

    private String messId;
    private String category; // GROCERY, UTILITY, GAS, RENT, MAINTENANCE, OTHER
    private String description;
    private double amount;
    private long date;
    private String paidById; // User who paid
    private String paidByName;
    private String receiptUrl; // Firebase storage URL for receipt image
    private boolean isSharedEqually; // If true, split equally among all members
    private boolean isIncludedInMealRate; // If true, include in meal rate calculation
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;
    private String pendingAction;

    public Expense() {
        this.id = "";
    }

    public Expense(@NonNull String id, String messId, String category,
                   String description, double amount, long date,
                   String paidById, String paidByName) {
        this.id = id;
        this.messId = messId;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.paidById = paidById;
        this.paidByName = paidByName;
        this.isSharedEqually = false;
        this.isIncludedInMealRate = "GROCERY".equals(category) || "GAS".equals(category);
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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getPaidById() { return paidById; }
    public void setPaidById(String paidById) { this.paidById = paidById; }

    public String getPaidByName() { return paidByName; }
    public void setPaidByName(String paidByName) { this.paidByName = paidByName; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public boolean isSharedEqually() { return isSharedEqually; }
    public void setSharedEqually(boolean sharedEqually) { isSharedEqually = sharedEqually; }

    public boolean isIncludedInMealRate() { return isIncludedInMealRate; }
    public void setIncludedInMealRate(boolean includedInMealRate) {
        isIncludedInMealRate = includedInMealRate;
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }

    // Category constants
    public static final String GROCERY = "GROCERY";
    public static final String UTILITY = "UTILITY";
    public static final String GAS = "GAS";
    public static final String RENT = "RENT";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String OTHER = "OTHER";
}
