package com.messkhata.service;

import android.content.Context;
import android.database.Cursor;

import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.Mess;

import java.util.Calendar;

/**
 * Service for automatic meal charging based on saved preferences
 * This service should be called daily (e.g., via WorkManager or AlarmManager)
 * to automatically charge users for meals based on their saved preferences
 */
public class MealAutoChargeService {

    private Context context;
    private MealDao mealDao;
    private MessDao messDao;

    public MealAutoChargeService(Context context) {
        this.context = context;
        this.mealDao = new MealDao(context);
        this.messDao = new MessDao(context);
    }

    /**
     * Process automatic meal charging for all users with preferences
     * This should be called once daily, preferably early morning
     * 
     * @param messId The mess ID to process auto-charging for
     * @return Number of users successfully charged
     */
    public int processAutoCharging(int messId) {
        int chargedCount = 0;
        
        try {
            long todayTimestamp = getTodayTimestamp();
            
            // Get all users with meal preferences for this mess
            Cursor cursor = mealDao.getActivePreferences(messId);
            
            while (cursor.moveToNext()) {
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow("userId"));
                int breakfast = cursor.getInt(cursor.getColumnIndexOrThrow("breakfast"));
                int lunch = cursor.getInt(cursor.getColumnIndexOrThrow("lunch"));
                int dinner = cursor.getInt(cursor.getColumnIndexOrThrow("dinner"));
                
                // Only charge if at least one meal is selected
                if (breakfast > 0 || lunch > 0 || dinner > 0) {
                    boolean charged = chargeTodayMeal(userId, messId, breakfast, lunch, dinner);
                    if (charged) {
                        chargedCount++;
                    }
                }
            }
            cursor.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return chargedCount;
    }

    /**
     * Charge a specific user for today's meals based on their preference
     * Only charges if not already charged for today
     * 
     * @param userId The user ID to charge
     * @param messId The mess ID
     * @param breakfast Breakfast preference (0 or 1)
     * @param lunch Lunch preference (0 or 1)
     * @param dinner Dinner preference (0 or 1)
     * @return true if successfully charged, false if already charged or error
     */
    public boolean chargeTodayMeal(int userId, int messId, int breakfast, int lunch, int dinner) {
        try {
            long todayTimestamp = getTodayTimestamp();
            
            // Check if already charged today
            Meal existingMeal = mealDao.getMealByDate(userId, todayTimestamp);
            if (existingMeal != null) {
                return false; // Already charged
            }
            
            // Get mess rate
            Mess mess = messDao.getMessByIdAsObject(messId);
            if (mess == null) {
                return false; // Mess not found
            }
            
            double mealRate = mess.getGroceryBudgetPerMeal() + mess.getCookingChargePerMeal();
            
            // Add meal entry with preference values
            return mealDao.addOrUpdateMeal(
                userId,
                messId,
                todayTimestamp,
                breakfast,
                lunch,
                dinner,
                mealRate
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get today's date timestamp at midnight (00:00:00)
     * @return Timestamp in seconds
     */
    private long getTodayTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / 1000; // Convert to seconds
    }
}
