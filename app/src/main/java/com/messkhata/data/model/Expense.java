package com.messkhata.data.model;

/**
 * Expense model class
 */
public class Expense {
    private int expenseId;
    private int messId;
    private int addedBy;
    private String category; // grocery, utilities, cleaning, gas, rent, miscellaneous
    private double amount;
    private String title; // Expense title (e.g., "Weekly Groceries")
    private String description; // Additional details (optional)
    private long expenseDate; // Unix timestamp
    private int memberCountAtTime; // Number of active members when expense was created
    private long createdAt; // Unix timestamp
    private String addedByName; // Name of person who added (for display)

    // Constructor
    public Expense() {
    }

    public Expense(int expenseId, int messId, int addedBy, String category, 
                   double amount, String title, String description, long expenseDate, 
                   int memberCountAtTime, long createdAt) {
        this.expenseId = expenseId;
        this.messId = messId;
        this.addedBy = addedBy;
        this.category = category;
        this.amount = amount;
        this.title = title;
        this.description = description;
        this.expenseDate = expenseDate;
        this.memberCountAtTime = memberCountAtTime;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getMessId() {
        return messId;
    }

    public void setMessId(int messId) {
        this.messId = messId;
    }

    public int getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(int addedBy) {
        this.addedBy = addedBy;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(long expenseDate) {
        this.expenseDate = expenseDate;
    }

    public int getMemberCountAtTime() {
        return memberCountAtTime;
    }

    public void setMemberCountAtTime(int memberCountAtTime) {
        this.memberCountAtTime = memberCountAtTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getAddedByName() {
        return addedByName;
    }

    public void setAddedByName(String addedByName) {
        this.addedByName = addedByName;
    }

    // Helper methods
    public boolean isGrocery() {
        return "grocery".equalsIgnoreCase(category);
    }

    public boolean isFixedExpense() {
        // Fixed expenses are split equally among members
        return "utilities".equalsIgnoreCase(category) ||
               "cleaning".equalsIgnoreCase(category) ||
               "gas".equalsIgnoreCase(category) ||
               "rent".equalsIgnoreCase(category) ||
               "miscellaneous".equalsIgnoreCase(category);
    }

    @Override
    public String toString() {
        return "Expense{" +
                "expenseId=" + expenseId +
                ", messId=" + messId +
                ", addedBy=" + addedBy +
                ", category='" + category + '\'' +
                ", amount=" + amount +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", expenseDate=" + expenseDate +
                ", memberCountAtTime=" + memberCountAtTime +
                ", createdAt=" + createdAt +
                ", addedByName='" + addedByName + '\'' +
                '}';
    }
}
