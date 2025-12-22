package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Meal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Data Access Object for Meal operations
 */
public class MealDao {

    private MessKhataDatabase dbHelper;

    public MealDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
    }

    /**
     * Add or update meal entry for a specific date
     * @return true if successful
     */
    public boolean addOrUpdateMeal(int userId, int messId, long date, 
                                   int breakfast, int lunch, int dinner, double mealRate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("userId", userId);
            values.put("messId", messId);
            values.put("mealDate", date);
            values.put("breakfast", breakfast);
            values.put("lunch", lunch);
            values.put("dinner", dinner);
            values.put("mealRate", mealRate);
            values.put("updatedAt", System.currentTimeMillis() / 1000);

            // Try to insert, if conflict (unique constraint) then update
            long result = db.insertWithOnConflict(
                MessKhataDatabase.TABLE_MEALS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            );

            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get meal entry for a specific date
     * @return Meal object or null if not found
     */
    public Meal getMealByDate(int userId, long date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MEALS +
                " WHERE userId = ? AND mealDate = ?";
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(userId),
            String.valueOf(date)
        });

        Meal meal = null;
        if (cursor.moveToFirst()) {
            meal = new Meal(
                cursor.getInt(cursor.getColumnIndexOrThrow("mealId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("userId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                cursor.getLong(cursor.getColumnIndexOrThrow("mealDate")),
                cursor.getInt(cursor.getColumnIndexOrThrow("breakfast")),
                cursor.getInt(cursor.getColumnIndexOrThrow("lunch")),
                cursor.getInt(cursor.getColumnIndexOrThrow("dinner")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("mealRate"))
            );
        }
        cursor.close();
        return meal;
    }

    /**
     * Get all meals for a user in a specific month
     * @return List of Meal objects
     */
    public List<Meal> getMealsByMonth(int userId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Meal> meals = new ArrayList<>();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MEALS +
                " WHERE userId = ? AND mealDate >= ? AND mealDate < ? " +
                " ORDER BY mealDate ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(userId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        while (cursor.moveToNext()) {
            Meal meal = new Meal(
                cursor.getInt(cursor.getColumnIndexOrThrow("mealId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("userId")),
                cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                cursor.getLong(cursor.getColumnIndexOrThrow("mealDate")),
                cursor.getInt(cursor.getColumnIndexOrThrow("breakfast")),
                cursor.getInt(cursor.getColumnIndexOrThrow("lunch")),
                cursor.getInt(cursor.getColumnIndexOrThrow("dinner")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("mealRate"))
            );
            meals.add(meal);
        }
        cursor.close();
        return meals;
    }

    /**
     * Get total meals for a user in a specific month
     * @return Total number of meals
     */
    public int getTotalMealsForMonth(int userId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM(breakfast + lunch + dinner) as total FROM " + 
                MessKhataDatabase.TABLE_MEALS +
                " WHERE userId = ? AND mealDate >= ? AND mealDate < ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(userId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        int total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Get total meals consumed by all members in a mess for a specific month
     * @return Total number of meals
     */
    public int getTotalMessMealsForMonth(int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM(breakfast + lunch + dinner) as total FROM " + 
                MessKhataDatabase.TABLE_MEALS +
                " WHERE messId = ? AND mealDate >= ? AND mealDate < ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(startDate),
            String.valueOf(endDate)
        });

        int total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Save meal preference for a user
     * @return true if successful
     */
    public boolean saveMealPreference(int userId, int messId, int breakfast, 
                                     int lunch, int dinner) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Calculate tomorrow's date as effectiveFrom
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long effectiveFrom = calendar.getTimeInMillis() / 1000;

            ContentValues values = new ContentValues();
            values.put("userId", userId);
            values.put("messId", messId);
            values.put("breakfast", breakfast);
            values.put("lunch", lunch);
            values.put("dinner", dinner);
            values.put("effectiveFrom", effectiveFrom);
            values.put("createdAt", System.currentTimeMillis() / 1000);

            long result = db.insert(MessKhataDatabase.TABLE_MEAL_PREFERENCES, null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get user's current meal preference
     * @return int array [breakfast, lunch, dinner] or null if no preference set
     */
    public int[] getMealPreference(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get the most recent preference
        String query = "SELECT breakfast, lunch, dinner FROM " + 
                MessKhataDatabase.TABLE_MEAL_PREFERENCES +
                " WHERE userId = ? ORDER BY createdAt DESC LIMIT 1";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        int[] preference = null;
        if (cursor.moveToFirst()) {
            preference = new int[3];
            preference[0] = cursor.getInt(0); // breakfast
            preference[1] = cursor.getInt(1); // lunch
            preference[2] = cursor.getInt(2); // dinner
        }
        cursor.close();
        return preference;
    }

    /**
     * Delete meal entry for a specific date
     * @return true if successful
     */
    public boolean deleteMeal(int userId, long date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete(MessKhataDatabase.TABLE_MEALS,
                "userId = ? AND mealDate = ?",
                new String[]{String.valueOf(userId), String.valueOf(date)});
        return rows > 0;
    }

    /**
     * Get total meal expense for a user in a specific month
     * @return Total meal expense amount
     */
    public double getTotalMealExpense(int userId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        String query = "SELECT SUM((breakfast + lunch + dinner) * mealRate) as totalExpense FROM " +
                MessKhataDatabase.TABLE_MEALS +
                " WHERE userId = ? AND mealDate >= ? AND mealDate < ?";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(userId),
                String.valueOf(startDate),
                String.valueOf(endDate)
        });

        double totalExpense = 0.0;
        if (cursor.moveToFirst()) {
            totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("totalExpense"));
        }
        cursor.close();
        return totalExpense;
    }

    /**
     * Get cumulative meal expense from user's join date to current date
     * Used for dashboard to show all meal expenses since user joined
     * @param userId The user ID
     * @param userJoinDate User's join date in seconds (Unix timestamp) - NOT USED, shows all meals
     * @return Total meal expense for all user's meals
     */
    public double getCumulativeMealExpenseFromJoinDate(int userId, long userJoinDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Show ALL user's meals regardless of join date
        // Personal meals belong to the user from when they were added
        String query = "SELECT SUM((breakfast + lunch + dinner) * mealRate) as totalExpense FROM " +
                MessKhataDatabase.TABLE_MEALS +
                " WHERE userId = ?";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(userId)
        });

        double totalExpense = 0.0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            totalExpense = cursor.getDouble(0);
        }
        cursor.close();
        return totalExpense;
    }

    /**
     * Get all active meal preferences for a specific mess
     * Returns the most recent preference for each user
     * Used by MealAutoChargeService for automatic daily charging
     * 
     * @param messId The mess ID
     * @return Cursor containing userId, breakfast, lunch, dinner for each user with preferences
     */
    public Cursor getActivePreferences(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get the most recent preference for each user in this mess
        // Using a subquery to get only the latest preference per user
        String query = "SELECT mp.userId, mp.breakfast, mp.lunch, mp.dinner " +
                "FROM " + MessKhataDatabase.TABLE_MEAL_PREFERENCES + " mp " +
                "INNER JOIN ( " +
                "    SELECT userId, MAX(createdAt) as maxCreated " +
                "    FROM " + MessKhataDatabase.TABLE_MEAL_PREFERENCES + " " +
                "    WHERE messId = ? " +
                "    GROUP BY userId " +
                ") latest ON mp.userId = latest.userId AND mp.createdAt = latest.maxCreated " +
                "WHERE mp.messId = ?";

        return db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(messId)});
    }
}
