package com.messkhata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Meal;
import com.messkhata.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Meal Fragment - Manage daily meals and preferences
 */
public class MealFragment extends Fragment {

    // UI Components - Header
    private TextView tvCurrentDate;
    private TextView tvDayOfWeek;
    
    // UI Components - Summary
    private TextView tvTotalMealsToday;
    private TextView tvBreakfastCount;
    private TextView tvLunchCount;
    private TextView tvDinnerCount;
    
    // UI Components - Meal Preference Counters
    private MaterialButton btnBreakfastMinus;
    private MaterialButton btnBreakfastPlus;
    private TextView tvBreakfastPrefCount;
    private MaterialButton btnLunchMinus;
    private MaterialButton btnLunchPlus;
    private TextView tvLunchPrefCount;
    private MaterialButton btnDinnerMinus;
    private MaterialButton btnDinnerPlus;
    private TextView tvDinnerPrefCount;
    private MaterialButton btnSavePreference;

    // DAOs
    private MealDao mealDao;
    
    // Session data
    private PreferenceManager prefManager;
    private long userId;
    private int messId;
    
    // Meal counts
    private int breakfastCount = 1;
    private int lunchCount = 1;
    private int dinnerCount = 1;
    
    // Current date
    private Calendar currentDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAO();
        loadSessionData();
        setupListeners();
        loadMealPreference();
        loadTodayMeals();
    }

    private void initializeViews(View view) {
        // Header
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate);
        tvDayOfWeek = view.findViewById(R.id.tvDayOfWeek);
        
        // Summary (using different IDs to avoid conflict)
        tvTotalMealsToday = view.findViewById(R.id.tvTotalMealsToday);
        
        // Preference counters
        btnBreakfastMinus = view.findViewById(R.id.btnBreakfastMinus);
        btnBreakfastPlus = view.findViewById(R.id.btnBreakfastPlus);
        tvBreakfastPrefCount = view.findViewById(R.id.tvBreakfastCount);
        
        btnLunchMinus = view.findViewById(R.id.btnLunchMinus);
        btnLunchPlus = view.findViewById(R.id.btnLunchPlus);
        tvLunchPrefCount = view.findViewById(R.id.tvLunchCount);
        
        btnDinnerMinus = view.findViewById(R.id.btnDinnerMinus);
        btnDinnerPlus = view.findViewById(R.id.btnDinnerPlus);
        tvDinnerPrefCount = view.findViewById(R.id.tvDinnerCount);
        
        btnSavePreference = view.findViewById(R.id.btnSavePreference);
    }

    private void initializeDAO() {
        mealDao = new MealDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = new PreferenceManager(requireContext());
        userId = Long.parseLong(prefManager.getUserId());
        messId = Integer.parseInt(prefManager.getMessId());
        currentDate = Calendar.getInstance();
        updateDateDisplay();
    }

    private void setupListeners() {
        // Breakfast counters
        btnBreakfastMinus.setOnClickListener(v -> updateCount("breakfast", -1));
        btnBreakfastPlus.setOnClickListener(v -> updateCount("breakfast", 1));
        
        // Lunch counters
        btnLunchMinus.setOnClickListener(v -> updateCount("lunch", -1));
        btnLunchPlus.setOnClickListener(v -> updateCount("lunch", 1));
        
        // Dinner counters
        btnDinnerMinus.setOnClickListener(v -> updateCount("dinner", -1));
        btnDinnerPlus.setOnClickListener(v -> updateCount("dinner", 1));
        
        // Save preference button
        btnSavePreference.setOnClickListener(v -> saveMealPreference());
    }

    private void updateCount(String mealType, int change) {
        switch (mealType) {
            case "breakfast":
                breakfastCount = Math.max(0, Math.min(5, breakfastCount + change));
                tvBreakfastPrefCount.setText(String.valueOf(breakfastCount));
                break;
            case "lunch":
                lunchCount = Math.max(0, Math.min(5, lunchCount + change));
                tvLunchPrefCount.setText(String.valueOf(lunchCount));
                break;
            case "dinner":
                dinnerCount = Math.max(0, Math.min(5, dinnerCount + change));
                tvDinnerPrefCount.setText(String.valueOf(dinnerCount));
                break;
        }
        
        // Update total and save meal for today
        updateTotalAndSave();
    }

    private void updateTotalAndSave() {
        int total = breakfastCount + lunchCount + dinnerCount;
        tvTotalMealsToday.setText(String.valueOf(total));
        
        // Save meal for today automatically
        saveTodayMeal();
    }

    private void saveTodayMeal() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long todayTimestamp = getTodayTimestamp();
                boolean success = mealDao.addOrUpdateMeal(
                    (int) userId,
                    messId,
                    todayTimestamp,
                    breakfastCount,
                    lunchCount,
                    dinnerCount
                );
                
                if (!success) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error saving meal", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void saveMealPreference() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                boolean success = mealDao.saveMealPreference(
                    (int) userId,
                    messId,
                    breakfastCount,
                    lunchCount,
                    dinnerCount
                );
                
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), 
                            "Preference saved! Will apply from tomorrow", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), 
                            "Error saving preference", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadMealPreference() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int[] preference = mealDao.getMealPreference((int) userId);
                
                requireActivity().runOnUiThread(() -> {
                    if (preference != null) {
                        breakfastCount = preference[0];
                        lunchCount = preference[1];
                        dinnerCount = preference[2];
                        
                        tvBreakfastPrefCount.setText(String.valueOf(breakfastCount));
                        tvLunchPrefCount.setText(String.valueOf(lunchCount));
                        tvDinnerPrefCount.setText(String.valueOf(dinnerCount));
                        
                        updateTotalAndSave();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadTodayMeals() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long todayTimestamp = getTodayTimestamp();
                Meal meal = mealDao.getMealByDate((int) userId, todayTimestamp);
                
                requireActivity().runOnUiThread(() -> {
                    if (meal != null) {
                        breakfastCount = meal.getBreakfast();
                        lunchCount = meal.getLunch();
                        dinnerCount = meal.getDinner();
                        
                        tvBreakfastPrefCount.setText(String.valueOf(breakfastCount));
                        tvLunchPrefCount.setText(String.valueOf(lunchCount));
                        tvDinnerPrefCount.setText(String.valueOf(dinnerCount));
                        tvTotalMealsToday.setText(String.valueOf(meal.getTotalMeals()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        
        tvCurrentDate.setText(monthFormat.format(currentDate.getTime()));
        tvDayOfWeek.setText(dayFormat.format(currentDate.getTime()));
    }

    private long getTodayTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / 1000; // Convert to seconds
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayMeals();
    }
}
