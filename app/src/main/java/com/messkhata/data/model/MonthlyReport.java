package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * MonthlyReport entity for storing calculated monthly summaries.
 */
@Entity(tableName = "monthly_reports",
        indices = {
            @Index(value = {"messId", "year", "month"}, unique = true)
        })
public class MonthlyReport {

    @PrimaryKey
    @NonNull
    private String id;

    private String messId;
    private int year;
    private int month; // 1-12
    private int totalMeals;
    private double totalMealExpenses; // Expenses included in meal rate
    private double totalFixedExpenses; // Expenses shared equally
    private double mealRate; // totalMealExpenses / totalMeals
    private boolean isFinalized;
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;
    private String pendingAction;

    public MonthlyReport() {
        this.id = "";
    }

    public MonthlyReport(@NonNull String id, String messId, int year, int month) {
        this.id = id;
        this.messId = messId;
        this.year = year;
        this.month = month;
        this.totalMeals = 0;
        this.totalMealExpenses = 0;
        this.totalFixedExpenses = 0;
        this.mealRate = 0;
        this.isFinalized = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.pendingAction = "CREATE";
    }

    // Calculate meal rate
    public void calculateMealRate() {
        if (totalMeals > 0) {
            this.mealRate = totalMealExpenses / totalMeals;
        } else {
            this.mealRate = 0;
        }
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getMessId() { return messId; }
    public void setMessId(String messId) { this.messId = messId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getTotalMeals() { return totalMeals; }
    public void setTotalMeals(int totalMeals) { this.totalMeals = totalMeals; }

    public double getTotalMealExpenses() { return totalMealExpenses; }
    public void setTotalMealExpenses(double totalMealExpenses) {
        this.totalMealExpenses = totalMealExpenses;
    }

    public double getTotalFixedExpenses() { return totalFixedExpenses; }
    public void setTotalFixedExpenses(double totalFixedExpenses) {
        this.totalFixedExpenses = totalFixedExpenses;
    }

    public double getMealRate() { return mealRate; }
    public void setMealRate(double mealRate) { this.mealRate = mealRate; }

    public boolean isFinalized() { return isFinalized; }
    public void setFinalized(boolean finalized) { isFinalized = finalized; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }
}
