package com.messkhata.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.messkhata.data.model.Meal;
import com.messkhata.data.repository.MealRepository;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * ViewModel for Meal tracking screen.
 */
public class MealViewModel extends AndroidViewModel {

    private final MealRepository mealRepository;
    private final PreferenceManager prefManager;

    public MealViewModel(@NonNull Application application) {
        super(application);
        mealRepository = new MealRepository(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get meals for a specific date (all members).
     */
    public LiveData<List<Meal>> getMealsByDate(long date) {
        String messId = prefManager.getMessId();
        return mealRepository.getMealsByMessAndDate(messId, date);
    }

    /**
     * Get meals for current user on a specific date.
     */
    public LiveData<List<Meal>> getMyMealsByDate(long date) {
        String userId = prefManager.getUserId();
        return mealRepository.getMealsByUserAndDate(userId, date);
    }

    /**
     * Get today's meals for all members.
     */
    public LiveData<List<Meal>> getTodayMeals() {
        return getMealsByDate(DateUtils.getTodayStart());
    }

    /**
     * Toggle a meal entry (0 <-> 1).
     */
    public void toggleMeal(String userId, String userName, long date, String mealType) {
        mealRepository.toggleMeal(userId, userName, date, mealType);
    }

    /**
     * Save a meal with specific count.
     */
    public void saveMeal(String userId, String userName, long date,
                         String mealType, int count, String notes) {
        mealRepository.saveMeal(userId, userName, date, mealType, count, notes);
    }

    /**
     * Quick toggle for current user.
     */
    public void toggleMyMeal(long date, String mealType) {
        String userId = prefManager.getUserId();
        String userName = prefManager.getUserName();
        mealRepository.toggleMeal(userId, userName, date, mealType);
    }

    /**
     * Check if current user can edit meals for others.
     * Admins and managers can edit anyone's meals.
     */
    public boolean canEditOthersMeals() {
        return prefManager.isAdminOrManager();
    }

    /**
     * Get the user's role.
     */
    public String getUserRole() {
        return prefManager.getUserRole();
    }
}
