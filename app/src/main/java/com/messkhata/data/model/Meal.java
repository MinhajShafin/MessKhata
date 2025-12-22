package com.messkhata.data.model;

/**
 * Meal model class
 */
public class Meal {
    private int mealId;
    private int userId;
    private int messId;
    private long mealDate; // Unix timestamp (date only, time set to 00:00:00)
    private int breakfast;
    private int lunch;
    private int dinner;
    private int totalMeals; // breakfast + lunch + dinner
    private double mealRate; // Fixed rate at time of consumption
    private double mealExpense; // totalMeals × mealRate

    // Constructor
    public Meal() {
    }

    public Meal(int mealId, int userId, int messId, long mealDate, 
                int breakfast, int lunch, int dinner) {
        this.mealId = mealId;
        this.userId = userId;
        this.messId = messId;
        this.mealDate = mealDate;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.totalMeals = breakfast + lunch + dinner;
        this.mealRate = 50.00; // Default rate
        this.mealExpense = this.totalMeals * this.mealRate;
    }

    public Meal(int mealId, int userId, int messId, long mealDate, 
                int breakfast, int lunch, int dinner, double mealRate) {
        this.mealId = mealId;
        this.userId = userId;
        this.messId = messId;
        this.mealDate = mealDate;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.totalMeals = breakfast + lunch + dinner;
        this.mealRate = mealRate;
        this.mealExpense = this.totalMeals * this.mealRate;
    }

    // Getters and Setters
    public int getMealId() {
        return mealId;
    }

    public void setMealId(int mealId) {
        this.mealId = mealId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMessId() {
        return messId;
    }

    public void setMessId(int messId) {
        this.messId = messId;
    }

    public long getMealDate() {
        return mealDate;
    }

    public void setMealDate(long mealDate) {
        this.mealDate = mealDate;
    }

    public int getBreakfast() {
        return breakfast;
    }

    public void setBreakfast(int breakfast) {
        this.breakfast = breakfast;
        updateTotalMeals();
    }

    public int getLunch() {
        return lunch;
    }

    public void setLunch(int lunch) {
        this.lunch = lunch;
        updateTotalMeals();
    }

    public int getDinner() {
        return dinner;
    }

    public void setDinner(int dinner) {
        this.dinner = dinner;
        updateTotalMeals();
    }

    public int getTotalMeals() {
        return totalMeals;
    }

    public double getMealRate() {
        return mealRate;
    }

    public void setMealRate(double mealRate) {
        this.mealRate = mealRate;
        updateMealExpense();
    }

    public double getMealExpense() {
        return mealExpense;
    }

    // Helper method to update total meals
    private void updateTotalMeals() {
        this.totalMeals = breakfast + lunch + dinner;
        updateMealExpense();
    }

    // Helper method to update meal expense
    private void updateMealExpense() {
        this.mealExpense = this.totalMeals * this.mealRate;
    }

    @Override
    public String toString() {
        return "Meal{" +
                "mealId=" + mealId +
                ", userId=" + userId +
                ", messId=" + messId +
                ", mealDate=" + mealDate +
                ", breakfast=" + breakfast +
                ", lunch=" + lunch +
                ", dinner=" + dinner +
                ", totalMeals=" + totalMeals +
                ", mealRate=" + mealRate +
                ", mealExpense=" + mealExpense +
                '}';
    }
}
