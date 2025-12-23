package com.messkhata.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.R;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.Mess;
import com.messkhata.data.sync.RealtimeSyncManager;
import com.messkhata.data.sync.SyncManager;
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
    private TextView tvMealExpenseToday;
    private TextView tvBreakfastSummary;
    private TextView tvLunchSummary;
    private TextView tvDinnerSummary;

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

    // UI Components - Admin Meal Rate Section
    private MaterialCardView cardAdminMealRate;
    private TextView tvCurrentMealRate;
    private TextInputEditText etGroceryBudget;
    private TextInputEditText etCookingCharge;
    private MaterialButton btnUpdateMealRate;

    // DAOs
    private MealDao mealDao;
    private MessDao messDao;

    // Session data
    private PreferenceManager prefManager;
    private long userId;
    private int messId;
    private String userRole;

    // Meal counts
    private int breakfastCount = 1;
    private int lunchCount = 1;
    private int dinnerCount = 1;

    // Broadcast receiver for real-time updates
    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Data updated from cloud - refresh UI
            if (isAdded() && getActivity() != null) {
                loadTodayMeals();
            }
        }
    };

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
        tvMealExpenseToday = view.findViewById(R.id.tvMealExpenseToday);
        tvBreakfastSummary = view.findViewById(R.id.tvBreakfastSummary);
        tvLunchSummary = view.findViewById(R.id.tvLunchSummary);
        tvDinnerSummary = view.findViewById(R.id.tvDinnerSummary);

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

        // Admin meal rate section
        cardAdminMealRate = view.findViewById(R.id.cardAdminMealRate);
        tvCurrentMealRate = view.findViewById(R.id.tvCurrentMealRate);
        etGroceryBudget = view.findViewById(R.id.etGroceryBudget);
        etCookingCharge = view.findViewById(R.id.etCookingCharge);
        btnUpdateMealRate = view.findViewById(R.id.btnUpdateMealRate);
    }

    private void initializeDAO() {
        mealDao = new MealDao(requireContext());
        messDao = new MessDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = PreferenceManager.getInstance(requireContext());

        // Check if session exists
        String userIdStr = prefManager.getUserId();
        String messIdStr = prefManager.getMessId();

        if (userIdStr == null || messIdStr == null) {
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        userId = Long.parseLong(userIdStr);
        messId = Integer.parseInt(messIdStr);
        userRole = prefManager.getUserRole();
        currentDate = Calendar.getInstance();
        updateDateDisplay();

        // Show admin section if user is admin (case-insensitive check)
        if (userRole != null && userRole.equalsIgnoreCase("admin")) {
            cardAdminMealRate.setVisibility(View.VISIBLE);
            loadCurrentMealRates();
        }
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

        // Admin update meal rate button
        btnUpdateMealRate.setOnClickListener(v -> updateMealRates());
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

        // Calculate and display meal expense
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Mess mess = messDao.getMessByIdAsObject(messId);
                if (mess != null) {
                    double mealRate = mess.getGroceryBudgetPerMeal() + mess.getCookingChargePerMeal();
                    double mealExpense = total * mealRate;

                    requireActivity().runOnUiThread(() -> {
                        tvMealExpenseToday.setText(String.format(java.util.Locale.getDefault(),
                                "৳ %.2f", mealExpense));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Save meal for today automatically
        saveTodayMeal();
    }

    private void saveTodayMeal() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get mess rate from Mess table
                Mess mess = messDao.getMessByIdAsObject(messId);
                if (mess == null) {
                    requireActivity().runOnUiThread(
                            () -> Toast.makeText(requireContext(), "Error: Mess not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                double mealRate = mess.getGroceryBudgetPerMeal() + mess.getCookingChargePerMeal();
                long todayTimestamp = getTodayTimestamp();

                boolean success = mealDao.addOrUpdateMeal(
                        (int) userId,
                        messId,
                        todayTimestamp,
                        breakfastCount,
                        lunchCount,
                        dinnerCount,
                        mealRate);

                if (!success) {
                    requireActivity().runOnUiThread(
                            () -> Toast.makeText(requireContext(), "Error saving meal", Toast.LENGTH_SHORT).show());
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
                        dinnerCount);

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
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
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

    /**
     * Load current meal rates from database (Admin only)
     */
    private void loadCurrentMealRates() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Mess mess = messDao.getMessByIdAsObject(messId);
                if (mess != null) {
                    double grocery = mess.getGroceryBudgetPerMeal();
                    double cooking = mess.getCookingChargePerMeal();
                    double total = grocery + cooking;

                    requireActivity().runOnUiThread(() -> {
                        tvCurrentMealRate.setText(String.format(Locale.getDefault(),
                                "Current Rate: ৳ %.2f per meal", total));
                        etGroceryBudget.setText(String.valueOf(grocery));
                        etCookingCharge.setText(String.valueOf(cooking));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Update meal rates (Admin only)
     */
    private void updateMealRates() {
        String groceryStr = etGroceryBudget.getText() != null ? etGroceryBudget.getText().toString().trim() : "";
        String cookingStr = etCookingCharge.getText() != null ? etCookingCharge.getText().toString().trim() : "";

        android.util.Log.d("MealFragment", "Update clicked - Grocery: " + groceryStr + ", Cooking: " + cookingStr);

        if (groceryStr.isEmpty() || cookingStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double grocery = Double.parseDouble(groceryStr);
            double cooking = Double.parseDouble(cookingStr);

            android.util.Log.d("MealFragment", "Parsed values - Grocery: " + grocery + ", Cooking: " + cooking + ", MessId: " + messId);

            if (grocery < 0 || cooking < 0) {
                Toast.makeText(requireContext(), "Rates cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            if (grocery + cooking == 0) {
                Toast.makeText(requireContext(), "Total rate cannot be zero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update in database
            MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    android.util.Log.d("MealFragment", "Calling updateMessRates with messId: " + messId);
                    boolean success = messDao.updateMessRates(messId, grocery, cooking);
                    android.util.Log.d("MealFragment", "Update result: " + success);

                    if (success) {
                        // Update today's meal rate for all members
                        double newRate = grocery + cooking;
                        long todayTimestamp = getTodayTimestamp();
                        mealDao.updateMealRateForDate(messId, todayTimestamp, newRate);
                        android.util.Log.d("MealFragment", "Updated today's meal rate to: " + newRate);
                    }

                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            double total = grocery + cooking;
                            tvCurrentMealRate.setText(String.format(Locale.getDefault(),
                                    "Current Rate: ৳ %.2f per meal", total));
                            Toast.makeText(requireContext(),
                                    "Meal rate updated successfully! New rate: ৳" + String.format(Locale.getDefault(), "%.2f", total),
                                    Toast.LENGTH_LONG).show();

                            // Sync meal rate to cloud for other members
                            try {
                                SyncManager.getInstance(requireContext()).syncMessImmediate(messId);
                            } catch (Exception syncEx) {
                                android.util.Log.e("MealFragment", "Sync error: " + syncEx.getMessage());
                            }

                            // Refresh meal expense display
                            updateTotalAndSave();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to update meal rate",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("MealFragment", "Error updating meal rate", e);
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for real-time sync updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(RealtimeSyncManager.ACTION_DATA_UPDATED);
        filter.addAction(RealtimeSyncManager.ACTION_MEALS_UPDATED);
        filter.addAction(RealtimeSyncManager.ACTION_MESS_UPDATED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(syncReceiver, filter);

        loadTodayMeals();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(syncReceiver);
    }
}
