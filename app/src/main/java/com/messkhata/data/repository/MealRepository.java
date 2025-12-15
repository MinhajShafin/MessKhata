package com.messkhata.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.messkhata.data.dao.MealDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Meal;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.utils.Constants;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.IdGenerator;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * Repository for Meal data operations.
 * Handles both local database and Firebase synchronization.
 */
public class MealRepository {

    private final MealDao mealDao;
    private final FirebaseSyncManager syncManager;
    private final NetworkUtils networkUtils;
    private final PreferenceManager prefManager;

    public MealRepository(Application application) {
        MessKhataDatabase database = MessKhataDatabase.getInstance(application);
        mealDao = database.mealDao();
        syncManager = FirebaseSyncManager.getInstance(database);
        networkUtils = NetworkUtils.getInstance(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get meals for a specific user on a specific date.
     */
    public LiveData<List<Meal>> getMealsByUserAndDate(String userId, long date) {
        long startOfDay = DateUtils.getStartOfDay(date);
        return mealDao.getMealsByUserAndDate(userId, startOfDay);
    }

    /**
     * Get all meals for the mess on a specific date.
     */
    public LiveData<List<Meal>> getMealsByMessAndDate(String messId, long date) {
        long startOfDay = DateUtils.getStartOfDay(date);
        return mealDao.getMealsByMessAndDate(messId, startOfDay);
    }

    /**
     * Get meals for a date range (for reports).
     */
    public List<Meal> getMealsByMessAndDateRange(String messId, long startDate, long endDate) {
        return mealDao.getMealsByMessAndDateRange(messId, startDate, endDate);
    }

    /**
     * Insert or update a meal entry.
     * Uses quick-entry logic: if entry exists, update; otherwise, create.
     */
    public void saveMeal(String userId, String userName, long date,
                         String mealType, int count, String notes) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String messId = prefManager.getMessId();
            long startOfDay = DateUtils.getStartOfDay(date);

            // Check if meal entry exists
            Meal existingMeal = mealDao.getMealEntry(userId, startOfDay, mealType);

            if (existingMeal != null) {
                // Update existing meal
                existingMeal.setCount(count);
                existingMeal.setNotes(notes);
                existingMeal.setUpdatedAt(System.currentTimeMillis());
                existingMeal.setSynced(false);
                existingMeal.setPendingAction(Constants.ACTION_UPDATE);
                mealDao.update(existingMeal);
            } else {
                // Create new meal
                String mealId = IdGenerator.generateId();
                Meal meal = new Meal(mealId, messId, userId, userName,
                                    startOfDay, mealType, count);
                meal.setNotes(notes);
                meal.setCreatedBy(prefManager.getUserId());
                mealDao.insert(meal);
            }

            // Trigger sync if online
            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Quick toggle meal entry (0 or 1).
     */
    public void toggleMeal(String userId, String userName, long date, String mealType) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String messId = prefManager.getMessId();
            long startOfDay = DateUtils.getStartOfDay(date);

            Meal existingMeal = mealDao.getMealEntry(userId, startOfDay, mealType);

            if (existingMeal != null) {
                // Toggle: if count > 0, set to 0; otherwise set to 1
                int newCount = existingMeal.getCount() > 0 ? 0 : 1;
                existingMeal.setCount(newCount);
                existingMeal.setUpdatedAt(System.currentTimeMillis());
                existingMeal.setSynced(false);
                existingMeal.setPendingAction(Constants.ACTION_UPDATE);
                mealDao.update(existingMeal);
            } else {
                // Create new meal with count = 1
                String mealId = IdGenerator.generateId();
                Meal meal = new Meal(mealId, messId, userId, userName,
                                    startOfDay, mealType, 1);
                meal.setCreatedBy(prefManager.getUserId());
                mealDao.insert(meal);
            }

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Bulk insert meals (for initial sync or batch operations).
     */
    public void insertMeals(List<Meal> meals) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            mealDao.insertAll(meals);
        });
    }

    /**
     * Delete a meal entry.
     */
    public void deleteMeal(Meal meal) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            meal.setSynced(false);
            meal.setPendingAction(Constants.ACTION_DELETE);
            meal.setUpdatedAt(System.currentTimeMillis());
            mealDao.update(meal);

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Get total meal count for a user in a date range.
     */
    public int getUserMealCountInRange(String userId, String messId,
                                        long startDate, long endDate) {
        return mealDao.getUserMealCountInRange(userId, messId, startDate, endDate);
    }

    /**
     * Get total meal count for the mess in a date range.
     */
    public int getTotalMealCountInRange(String messId, long startDate, long endDate) {
        return mealDao.getTotalMealCountInRange(messId, startDate, endDate);
    }
}
