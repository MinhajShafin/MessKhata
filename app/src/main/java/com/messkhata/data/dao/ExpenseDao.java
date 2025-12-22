package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Expense;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Data Access Object for Expense operations
 */
public class ExpenseDao {

    private MessKhataDatabase dbHelper;

    public ExpenseDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
    }

    /**
     * Add new expense
     * @param expenseDate Date in milliseconds (will be converted to seconds for storage)
     * @return expenseId if successful, -1 if failed
     */
    public long addExpense(int messId, int addedBy, String category, 
                          double amount, String title, String description, long expenseDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("messId", messId);
            values.put("addedBy", addedBy);
            values.put("category", category);
            values.put("amount", amount);
            values.put("title", title);
            values.put("description", description);
            values.put("expenseDate", expenseDate / 1000); // Convert milliseconds to seconds
            values.put("createdAt", System.currentTimeMillis() / 1000);
            values.put("updatedAt", System.currentTimeMillis() / 1000);

            return db.insert(MessKhataDatabase.TABLE_EXPENSES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Get all expenses for a mess in a specific month
     * @return List of Expense objects with addedByName populated
     */
    public List<Expense> getExpensesByMonth(int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Expense> expenses = new ArrayList<>();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT e.*, u.fullName as addedByName " +
                "FROM " + MessKhataDatabase.TABLE_EXPENSES + " e " +
                "LEFT JOIN " + MessKhataDatabase.TABLE_USERS + " u " +
                "ON e.addedBy = u.userId " +
                "WHERE e.messId = ? AND e.expenseDate >= ? AND e.expenseDate < ? " +
                "ORDER BY e.expenseDate DESC, e.createdAt DESC";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        while (cursor.moveToNext()) {
            Expense expense = new Expense(
                cursor.getInt(cursor.getColumnIndexOrThrow("expenseId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("addedBy")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("expenseDate")),
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            );
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
            expenses.add(expense);
        }
        cursor.close();
        return expenses;
    }

    /**
     * Get expenses filtered by category for a specific month
     * @return List of Expense objects
     */
    public List<Expense> getExpensesByCategory(int messId, String category, 
                                               int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Expense> expenses = new ArrayList<>();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT e.*, u.fullName as addedByName " +
                "FROM " + MessKhataDatabase.TABLE_EXPENSES + " e " +
                "LEFT JOIN " + MessKhataDatabase.TABLE_USERS + " u " +
                "ON e.addedBy = u.userId " +
                "WHERE e.messId = ? AND e.category = ? " +
                "AND e.expenseDate >= ? AND e.expenseDate < ? " +
                "ORDER BY e.expenseDate DESC";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            category,
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        while (cursor.moveToNext()) {
            Expense expense = new Expense(
                cursor.getInt(cursor.getColumnIndexOrThrow("expenseId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("addedBy")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("expenseDate")),
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            );
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
            expenses.add(expense);
        }
        cursor.close();
        return expenses;
    }

    /**
     * Get total expense amount by category for a specific month
     * @return Total amount
     */
    public double getTotalExpenseByCategory(int messId, String category, 
                                           int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM(amount) as total FROM " + 
                MessKhataDatabase.TABLE_EXPENSES +
                " WHERE messId = ? AND category = ? " +
                "AND expenseDate >= ? AND expenseDate < ?";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            category,
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
     * Update expense
     * @return true if successful
     */
    public boolean updateExpense(int expenseId, double amount, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("description", description);
        values.put("updatedAt", System.currentTimeMillis() / 1000);

        int rows = db.update(MessKhataDatabase.TABLE_EXPENSES,
                values,
                "expenseId = ?",
                new String[]{String.valueOf(expenseId)});
        return rows > 0;
    }

    /**
     * Delete expense (admin only - caller should verify permissions)
     * @return true if successful
     */
    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete(MessKhataDatabase.TABLE_EXPENSES,
                "expenseId = ?",
                new String[]{String.valueOf(expenseId)});
        return rows > 0;
    }

    /**
     * Get expense by ID
     * @return Expense object or null if not found
     */
    public Expense getExpenseById(int expenseId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT e.*, u.fullName as addedByName " +
                "FROM " + MessKhataDatabase.TABLE_EXPENSES + " e " +
                "LEFT JOIN " + MessKhataDatabase.TABLE_USERS + " u " +
                "ON e.addedBy = u.userId " +
                "WHERE e.expenseId = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(expenseId)});

        Expense expense = null;
        if (cursor.moveToFirst()) {
            expense = new Expense(
                cursor.getInt(cursor.getColumnIndexOrThrow("expenseId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("addedBy")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("expenseDate")),
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            );
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
        }
        cursor.close();
        return expense;
    }

    /**
     * Get total expenses for a mess in a specific month
     * Used for calculating shared expenses among members
     * @return Total expense amount
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
}
