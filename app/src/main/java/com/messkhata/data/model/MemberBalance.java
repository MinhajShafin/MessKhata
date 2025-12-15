package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * MemberBalance entity for storing individual member's monthly balance.
 * Tracks how much each member owes or is owed.
 */
@Entity(tableName = "member_balances",
        indices = {
            @Index(value = {"messId", "userId", "year", "month"}, unique = true),
            @Index(value = {"reportId"})
        })
public class MemberBalance {

    @PrimaryKey
    @NonNull
    private String id;

    private String messId;
    private String userId;
    private String userName;
    private String reportId; // Reference to MonthlyReport
    private int year;
    private int month;
    private int totalMeals; // Total meals consumed by this member
    private double mealCost; // totalMeals * mealRate
    private double fixedShare; // Member's share of fixed expenses
    private double totalDue; // mealCost + fixedShare
    private double totalPaid; // Total expenses paid by this member
    private double balance; // totalPaid - totalDue (positive = owed to member, negative = member owes)
    private boolean isSettled;
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;
    private String pendingAction;

    public MemberBalance() {
        this.id = "";
    }

    public MemberBalance(@NonNull String id, String messId, String userId,
                         String userName, String reportId, int year, int month) {
        this.id = id;
        this.messId = messId;
        this.userId = userId;
        this.userName = userName;
        this.reportId = reportId;
        this.year = year;
        this.month = month;
        this.totalMeals = 0;
        this.mealCost = 0;
        this.fixedShare = 0;
        this.totalDue = 0;
        this.totalPaid = 0;
        this.balance = 0;
        this.isSettled = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.pendingAction = "CREATE";
    }

    // Calculate balance
    public void calculateBalance(double mealRate, double fixedExpensePerMember) {
        this.mealCost = totalMeals * mealRate;
        this.fixedShare = fixedExpensePerMember;
        this.totalDue = mealCost + fixedShare;
        this.balance = totalPaid - totalDue;
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

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getTotalMeals() { return totalMeals; }
    public void setTotalMeals(int totalMeals) { this.totalMeals = totalMeals; }

    public double getMealCost() { return mealCost; }
    public void setMealCost(double mealCost) { this.mealCost = mealCost; }

    public double getFixedShare() { return fixedShare; }
    public void setFixedShare(double fixedShare) { this.fixedShare = fixedShare; }

    public double getTotalDue() { return totalDue; }
    public void setTotalDue(double totalDue) { this.totalDue = totalDue; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public boolean isSettled() { return isSettled; }
    public void setSettled(boolean settled) { isSettled = settled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }
}
