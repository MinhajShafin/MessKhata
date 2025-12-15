package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.Meal;

import java.util.List;

@Dao
public interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Meal meal);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Meal> meals);

    @Update
    void update(Meal meal);

    @Delete
    void delete(Meal meal);

    @Query("SELECT * FROM meals WHERE id = :mealId")
    LiveData<Meal> getMealById(String mealId);

    @Query("SELECT * FROM meals WHERE id = :mealId")
    Meal getMealByIdSync(String mealId);

    // Get meals for a specific user on a specific date
    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date")
    LiveData<List<Meal>> getMealsByUserAndDate(String userId, long date);

    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date")
    List<Meal> getMealsByUserAndDateSync(String userId, long date);

    // Get all meals for a mess on a specific date
    @Query("SELECT * FROM meals WHERE messId = :messId AND date = :date ORDER BY userName")
    LiveData<List<Meal>> getMealsByMessAndDate(String messId, long date);

    @Query("SELECT * FROM meals WHERE messId = :messId AND date = :date")
    List<Meal> getMealsByMessAndDateSync(String messId, long date);

    // Get meals for a date range (for monthly reports)
    @Query("SELECT * FROM meals WHERE messId = :messId AND date >= :startDate AND date <= :endDate")
    List<Meal> getMealsByMessAndDateRange(String messId, long startDate, long endDate);

    // Get total meal count for a user in a date range
    @Query("SELECT SUM(count) FROM meals WHERE userId = :userId AND messId = :messId AND date >= :startDate AND date <= :endDate")
    int getUserMealCountInRange(String userId, String messId, long startDate, long endDate);

    // Get total meal count for mess in a date range
    @Query("SELECT SUM(count) FROM meals WHERE messId = :messId AND date >= :startDate AND date <= :endDate")
    int getTotalMealCountInRange(String messId, long startDate, long endDate);

    // Get unsynced meals
    @Query("SELECT * FROM meals WHERE isSynced = 0")
    List<Meal> getUnsyncedMeals();

    @Query("UPDATE meals SET isSynced = 1, pendingAction = NULL WHERE id = :mealId")
    void markAsSynced(String mealId);

    @Query("UPDATE meals SET pendingAction = :action, isSynced = 0, updatedAt = :timestamp WHERE id = :mealId")
    void markForSync(String mealId, String action, long timestamp);

    @Query("DELETE FROM meals WHERE id = :mealId")
    void deleteById(String mealId);

    // Check if meal entry exists
    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date AND mealType = :mealType LIMIT 1")
    Meal getMealEntry(String userId, long date, String mealType);
}
