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
     * 
     * @param expenseDate       Date in milliseconds (will be converted to seconds
     *                          for storage)
     * @param memberCountAtTime Number of active members when expense was created
     * @return expenseId if successful, -1 if failed
     */
    public long addExpense(int messId, int addedBy, String category,
            double amount, String title, String description,
            long expenseDate, int memberCountAtTime) {
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
            values.put("memberCountAtTime", memberCountAtTime);
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
     * 
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

        Cursor cursor = db.rawQuery(query, new String[] {
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
                    cursor.getInt(cursor.getColumnIndexOrThrow("memberCountAtTime")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
            expenses.add(expense);
        }
        cursor.close();
        return expenses;
    }

    /**
     * Get expenses filtered by category for a specific month
     * 
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

        Cursor cursor = db.rawQuery(query, new String[] {
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
                    cursor.getInt(cursor.getColumnIndexOrThrow("memberCountAtTime")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
            expenses.add(expense);
        }
        cursor.close();
        return expenses;
    }

    /**
     * Get total expense amount by category for a specific month
     * 
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

        Cursor cursor = db.rawQuery(query, new String[] {
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
     * 
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
                new String[] { String.valueOf(expenseId) });
        return rows > 0;
    }

    /**
     * Delete expense (admin only - caller should verify permissions)
     * 
     * @return true if successful
     */
    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete(MessKhataDatabase.TABLE_EXPENSES,
                "expenseId = ?",
                new String[] { String.valueOf(expenseId) });
        return rows > 0;
    }

    /**
     * Get expense by ID
     * 
     * @return Expense object or null if not found
     */
    public Expense getExpenseById(int expenseId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT e.*, u.fullName as addedByName " +
                "FROM " + MessKhataDatabase.TABLE_EXPENSES + " e " +
                "LEFT JOIN " + MessKhataDatabase.TABLE_USERS + " u " +
                "ON e.addedBy = u.userId " +
                "WHERE e.expenseId = ?";

        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(expenseId) });

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
                    cursor.getInt(cursor.getColumnIndexOrThrow("memberCountAtTime")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")));
            expense.setAddedByName(cursor.getString(cursor.getColumnIndexOrThrow("addedByName")));
        }
        cursor.close();
        return expense;
    }

    /**
     * Get total expenses for a mess in a specific month
     * Used for calculating shared expenses among members
     * 
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

        Cursor cursor = db.rawQuery(query, new String[] {
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
     * Get total expenses for a user based on their join date
     * Only includes expenses that occurred on or after the user's join date
     * 
     * @param userJoinDate User's join date in seconds (Unix timestamp)
     * @return Total expense amount for the user's share
     */
    public double getTotalExpensesAfterDate(int messId, long userJoinDate, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        // Use the later of userJoinDate or month start
        long effectiveStartDate = Math.max(startDate, userJoinDate);

        String query = "SELECT SUM(amount) as total FROM " +
                MessKhataDatabase.TABLE_EXPENSES +
                " WHERE messId = ? AND expenseDate >= ? AND expenseDate < ?";

        Cursor cursor = db.rawQuery(query, new String[] {
                String.valueOf(messId),
                String.valueOf(effectiveStartDate),
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
     * Get cumulative total expenses from user's join date to current date
     * Used for dashboard to show all expenses since user joined
     * 
     * @param messId       The mess ID
     * @param userJoinDate User's join date in seconds (Unix timestamp)
     * @return Total expense amount since join date
     */
    public double getCumulativeExpensesFromJoinDate(int messId, long userJoinDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        long currentDate = System.currentTimeMillis() / 1000;

        String query = "SELECT SUM(amount) as total FROM " +
                MessKhataDatabase.TABLE_EXPENSES +
                " WHERE messId = ? AND expenseDate >= ? AND expenseDate <= ?";

        Cursor cursor = db.rawQuery(query, new String[] {
                String.valueOf(messId),
                String.valueOf(userJoinDate),
                String.valueOf(currentDate)
        });

        double total = 0.0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Get accurate user share of expenses from their join date onwards
     * Uses memberCountAtTime stored with each expense for precise calculation
     * New members only pay for expenses added AFTER they joined (fair distribution)
     * Uses > (strictly after) to prevent same-day joiners from seeing earlier expenses
     * @param messId The mess ID
     * @param userJoinDate User's join date in seconds (Unix timestamp)
     * @return User's share of expenses since they joined
     */
    public double getAccurateUserShareOfExpenses(int messId, long userJoinDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get expenses added AFTER user joined (strictly after, not equal)
        // This ensures same-day joiners don't pay for earlier expenses
        String query = "SELECT amount, memberCountAtTime FROM " + 
                MessKhataDatabase.TABLE_EXPENSES +
                " WHERE messId = ? AND expenseDate > ?";

        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(userJoinDate)
        });

        double userShare = 0.0;
        while (cursor.moveToNext()) {
            double amount = cursor.getDouble(0);
            int memberCount = cursor.getInt(1);

            // Calculate user's share of this expense
            if (memberCount > 0) {
                userShare += (amount / memberCount);
            }
        }
        cursor.close();
        return userShare;
    }

    /**
     * Add or update an expense from sync
     * Used for syncing expenses from Firebase to local database
     * 
     * @return true if successful
     */
    public boolean addOrUpdateExpense(int expenseId, int messId, int addedBy, String category,
            double amount, String title, String description,
            long expenseDate, int memberCountAtTime, long createdAt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Check if expense already exists by checking for same messId, addedBy,
            // expenseDate, and amount
            // (since expenseId might differ between devices)
            String checkQuery = "SELECT expenseId FROM " + MessKhataDatabase.TABLE_EXPENSES +
                    " WHERE messId = ? AND addedBy = ? AND expenseDate = ? AND amount = ? AND title = ?";
            Cursor cursor = db.rawQuery(checkQuery, new String[] {
                    String.valueOf(messId),
                    String.valueOf(addedBy),
                    String.valueOf(expenseDate),
                    String.valueOf(amount),
                    title
            });

            ContentValues values = new ContentValues();
            values.put("messId", messId);
            values.put("addedBy", addedBy);
            values.put("category", category);
            values.put("amount", amount);
            values.put("title", title);
            values.put("description", description);
            values.put("expenseDate", expenseDate);
            values.put("memberCountAtTime", memberCountAtTime);
            values.put("updatedAt", System.currentTimeMillis() / 1000);

            boolean success;
            if (cursor.moveToFirst()) {
                // Update existing expense
                int existingId = cursor.getInt(0);
                int rows = db.update(MessKhataDatabase.TABLE_EXPENSES, values,
                        "expenseId = ?", new String[] { String.valueOf(existingId) });
                success = rows > 0;
            } else {
                // Insert new expense
                values.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis() / 1000);
                long result = db.insert(MessKhataDatabase.TABLE_EXPENSES, null, values);
                success = result != -1;
            }
            cursor.close();
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
