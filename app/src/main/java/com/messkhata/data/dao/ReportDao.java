package com.messkhata.data.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.MemberBalance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Data Access Object for Report and Analytics operations
 */
public class ReportDao {

    private MessKhataDatabase dbHelper;
    private MealDao mealDao;
    private ExpenseDao expenseDao;

    public ReportDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
        this.mealDao = new MealDao(context);
        this.expenseDao = new ExpenseDao(context);
    }

    /**
     * Get member balances for a specific month
     * Calculates bill, payments, and due for each member
     * @return List of MemberBalance objects
     */
    public List<MemberBalance> getMemberBalances(int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MemberBalance> balances = new ArrayList<>();

        // Get all members of the mess
        String query = "SELECT userId, fullName FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE messId = ? AND isActive = 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});

        while (cursor.moveToNext()) {
            long userId = cursor.getLong(0);
            String fullName = cursor.getString(1);

            // Get total meals for this user
            int totalMeals = mealDao.getTotalMealsForMonth((int) userId, month, year);

            // For now, use estimated bill (actual calculation will be done in month-end)
            // Get mess rates
            double mealRate = getEstimatedMealRate(messId);
            double mealCost = totalMeals * mealRate;

            // Get shared expenses (utilities, cleaning, gas, rent, misc)
            double sharedExpenses = getSharedExpensesPerMember(messId, month, year);

            double totalBill = mealCost + sharedExpenses;

            // Get total paid (from payments table - for now 0)
            double totalPaid = getTotalPaid(userId, messId, month, year);

            MemberBalance balance = new MemberBalance(
                userId,
                fullName,
                totalMeals,
                totalBill,
                totalPaid
            );
            balances.add(balance);
        }
        cursor.close();
        return balances;
    }

    /**
     * Get total expenses for a mess in a specific month
     * @return Total amount across all categories
     */
    public double getTotalExpenses(int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM(amount) as total FROM " + 
                MessKhataDatabase.TABLE_EXPENSES +
                " WHERE messId = ? AND expenseDate >= ? AND expenseDate < ?";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        double total = 0.0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Get total grocery expenses for a specific month
     * @return Total grocery amount
     */
    public double getGroceryTotal(int messId, int month, int year) {
        return expenseDao.getTotalExpenseByCategory(messId, "grocery", month, year);
    }

    /**
     * Get total meals consumed by all members in a month
     * @return Total number of meals
     */
    public int getTotalMeals(int messId, int month, int year) {
        return mealDao.getTotalMessMealsForMonth(messId, month, year);
    }

    /**
     * Calculate actual meal rate for a specific month
     * Rate = (totalGrocery / totalMeals) + cookingCharge
     * @return Meal rate per meal
     */
    public double calculateMealRate(int messId, int month, int year) {
        double groceryTotal = getGroceryTotal(messId, month, year);
        int totalMeals = getTotalMeals(messId, month, year);

        if (totalMeals == 0) {
            return getEstimatedMealRate(messId); // Return estimated rate if no meals
        }

        double groceryRate = groceryTotal / totalMeals;
        double cookingCharge = getCookingCharge(messId);

        return groceryRate + cookingCharge;
    }

    /**
     * Get estimated meal rate (grocery budget + cooking charge)
     * Used before month-end calculation
     * @return Estimated meal rate
     */
    private double getEstimatedMealRate(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT groceryBudgetPerMeal, cookingChargePerMeal FROM " +
                MessKhataDatabase.TABLE_MESS + " WHERE messId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});

        double rate = 50.0; // Default
        if (cursor.moveToFirst()) {
            double groceryBudget = cursor.getDouble(0);
            double cookingCharge = cursor.getDouble(1);
            rate = groceryBudget + cookingCharge;
        }
        cursor.close();
        return rate;
    }

    /**
     * Get cooking charge from mess settings
     * @return Cooking charge per meal
     */
    private double getCookingCharge(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT cookingChargePerMeal FROM " +
                MessKhataDatabase.TABLE_MESS + " WHERE messId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});

        double charge = 10.0; // Default
        if (cursor.moveToFirst()) {
            charge = cursor.getDouble(0);
        }
        cursor.close();
        return charge;
    }

    /**
     * Calculate shared expenses per member
     * (utilities + cleaning + gas + rent + miscellaneous) / numberOfMembers
     * @return Per-member share of fixed expenses
     */
    private double getSharedExpensesPerMember(int messId, int month, int year) {
        double utilities = expenseDao.getTotalExpenseByCategory(messId, "utilities", month, year);
        double cleaning = expenseDao.getTotalExpenseByCategory(messId, "cleaning", month, year);
        double gas = expenseDao.getTotalExpenseByCategory(messId, "gas", month, year);
        double rent = expenseDao.getTotalExpenseByCategory(messId, "rent", month, year);
        double misc = expenseDao.getTotalExpenseByCategory(messId, "miscellaneous", month, year);

        double totalShared = utilities + cleaning + gas + rent + misc;
        int memberCount = getActiveMemberCount(messId);

        if (memberCount == 0) return 0;
        return totalShared / memberCount;
    }

    /**
     * Get number of active members in a mess
     * @return Member count
     */
    private int getActiveMemberCount(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT COUNT(*) FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE messId = ? AND isActive = 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Get total amount paid by a user for a specific month
     * @return Total paid amount
     */
    private double getTotalPaid(long userId, int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM(amount) as total FROM " + 
                MessKhataDatabase.TABLE_PAYMENTS +
                " WHERE userId = ? AND messId = ? " +
                "AND paidDate >= ? AND paidDate < ?";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(userId),
            String.valueOf(messId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        double total = 0.0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Get expense breakdown by category for a month
     * @return Array of [grocery, utilities, cleaning, gas, rent, miscellaneous]
     */
    public double[] getExpenseBreakdown(int messId, int month, int year) {
        double[] breakdown = new double[6];
        breakdown[0] = expenseDao.getTotalExpenseByCategory(messId, "grocery", month, year);
        breakdown[1] = expenseDao.getTotalExpenseByCategory(messId, "utilities", month, year);
        breakdown[2] = expenseDao.getTotalExpenseByCategory(messId, "cleaning", month, year);
        breakdown[3] = expenseDao.getTotalExpenseByCategory(messId, "gas", month, year);
        breakdown[4] = expenseDao.getTotalExpenseByCategory(messId, "rent", month, year);
        breakdown[5] = expenseDao.getTotalExpenseByCategory(messId, "miscellaneous", month, year);
        return breakdown;
    }
}
