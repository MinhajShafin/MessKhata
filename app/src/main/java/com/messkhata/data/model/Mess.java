package com.messkhata.data.model;

/**
 * Mess model class
 */
public class Mess {
    private int messId;
    private String messName;
    private double groceryBudgetPerMeal;
    private double cookingChargePerMeal;
    private long createdDate; // Unix timestamp

    // Constructor
    public Mess() {
    }

    public Mess(int messId, String messName, double groceryBudgetPerMeal, 
                double cookingChargePerMeal, long createdDate) {
        this.messId = messId;
        this.messName = messName;
        this.groceryBudgetPerMeal = groceryBudgetPerMeal;
        this.cookingChargePerMeal = cookingChargePerMeal;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public int getMessId() {
        return messId;
    }

    public void setMessId(int messId) {
        this.messId = messId;
    }

    public String getMessName() {
        return messName;
    }

    public void setMessName(String messName) {
        this.messName = messName;
    }

    public double getGroceryBudgetPerMeal() {
        return groceryBudgetPerMeal;
    }

    public void setGroceryBudgetPerMeal(double groceryBudgetPerMeal) {
        this.groceryBudgetPerMeal = groceryBudgetPerMeal;
    }

    public double getCookingChargePerMeal() {
        return cookingChargePerMeal;
    }

    public void setCookingChargePerMeal(double cookingChargePerMeal) {
        this.cookingChargePerMeal = cookingChargePerMeal;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    // Helper methods
    public String getInvitationCode() {
        return String.valueOf(messId + 999);
    }

    public double getFixedMealRate() {
        return groceryBudgetPerMeal + cookingChargePerMeal;
    }

    @Override
    public String toString() {
        return "Mess{" +
                "messId=" + messId +
                ", messName='" + messName + '\'' +
                ", groceryBudgetPerMeal=" + groceryBudgetPerMeal +
                ", cookingChargePerMeal=" + cookingChargePerMeal +
                ", createdDate=" + createdDate +
                '}';
    }
}
