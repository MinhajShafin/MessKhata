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
    private String description;
    private long expenseDate; // Unix timestamp
    private long createdAt; // Unix timestamp
    private String addedByName; // Name of person who added (for display)

    // Constructor
    public Expense() {
    }

    public Expense(int expenseId, int messId, int addedBy, String category, 
                   double amount, String description, long expenseDate, long createdAt) {
        this.expenseId = expenseId;
        this.messId = messId;
        this.addedBy = addedBy;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
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
                ", description='" + description + '\'' +
                ", expenseDate=" + expenseDate +
                ", createdAt=" + createdAt +
                ", addedByName='" + addedByName + '\'' +
                '}';
    }
}
