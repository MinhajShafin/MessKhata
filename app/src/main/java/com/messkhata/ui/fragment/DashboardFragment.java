package com.messkhata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.messkhata.R;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.ReportDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.User;
import com.messkhata.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard Fragment - Shows user summary and mess overview
 */
public class DashboardFragment extends Fragment {

    // UI Components
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvGreeting;
    private TextView tvUserName;
    private TextView tvRole;
    private TextView tvMessName;
    private TextView tvMemberCount;
    private TextView tvCurrentMealRate;
    private TextView tvTotalMeals;
    private TextView tvMonthlyExpenses;
    private ImageView ivSync;

    // DAOs
    private UserDao userDao;
    private MessDao messDao;
    private MealDao mealDao;
    private ExpenseDao expenseDao;
    private ReportDao reportDao;

    // Session data
    private PreferenceManager prefManager;
    private long userId;
    private int messId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAOs();
        loadSessionData();
        setupListeners();
        loadDashboardData();
    }

    private void initializeViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvRole = view.findViewById(R.id.tvRole);
        tvMessName = view.findViewById(R.id.tvMessName);
        tvMemberCount = view.findViewById(R.id.tvMemberCount);
        tvCurrentMealRate = view.findViewById(R.id.tvCurrentMealRate);
        tvTotalMeals = view.findViewById(R.id.tvTotalMeals);
        tvMonthlyExpenses = view.findViewById(R.id.tvMonthlyExpenses);
        ivSync = view.findViewById(R.id.ivSync);
    }

    private void initializeDAOs() {
        userDao = new UserDao(requireContext());
        messDao = new MessDao(requireContext());
        mealDao = new MealDao(requireContext());
        expenseDao = new ExpenseDao(requireContext());
        reportDao = new ReportDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = PreferenceManager.getInstance(requireContext());
        
        // Check if session exists
        String userIdStr = prefManager.getUserId();
        String messIdStr = prefManager.getMessId();
        
        if (userIdStr == null || messIdStr == null) {
            // Session invalid, redirect to login
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }
        
        userId = Long.parseLong(userIdStr);
        messId = Integer.parseInt(messIdStr);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);
        ivSync.setOnClickListener(v -> loadDashboardData());
    }

    private void loadDashboardData() {
        // Check if views are initialized
        if (swipeRefresh == null || tvUserName == null) {
            return;
        }
        
        swipeRefresh.setRefreshing(true);

        // Load data in background thread
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get user data
                User user = userDao.getUserByIdAsObject(userId);
                
                // Get mess data
                Mess mess = messDao.getMessByIdAsObject(messId);
                
                // Get current month
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH) + 1;
                int currentYear = calendar.get(Calendar.YEAR);
                
                // Get user's total meals this month
                int totalMeals = mealDao.getTotalMealsForMonth((int) userId, currentMonth, currentYear);
                
                // Get user's total meal expense (using actual mealRate from Meals table)
                double totalMealExpense = mealDao.getTotalMealExpense((int) userId, currentMonth, currentYear);
                
                // Get user's joined date
                long userJoinDate = user != null ? user.getJoinedDate() : 0;
                
                // Get total expenses that occurred AFTER user joined
                double userRelevantExpenses = expenseDao.getTotalExpensesAfterDate(messId, userJoinDate, currentMonth, currentYear);
                
                // Get count of members who were active when those expenses occurred
                // For simplicity, we'll use current active member count
                // In a more accurate system, we'd calculate per-expense member count
                int activeMemberCount = userDao.getActiveMemberCount(messId, currentMonth, currentYear);
                
                // Calculate user's share of expenses (only expenses after they joined)
                double sharedExpense = (activeMemberCount > 0) ? (userRelevantExpenses / activeMemberCount) : 0.0;
                
                // Calculate total expense (meal + shared)
                double totalExpense = totalMealExpense + sharedExpense;
                
                // Get member count
                List<User> members = userDao.getMembersByMessId(messId);
                int memberCount = members.size();
                
                // Get current meal rate
                double mealRate = mess != null ? mess.getFixedMealRate() : 50.0;

                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    updateUI(user, mess, totalMeals, totalMealExpense, totalExpense, 
                            memberCount, mealRate);
                    swipeRefresh.setRefreshing(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), 
                            "Error loading dashboard: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void updateUI(User user, Mess mess, int totalMeals, double totalMealExpense,
                         double totalExpense, int memberCount, double mealRate) {
        // Set greeting based on time of day
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        tvGreeting.setText(greeting);

        // Set user info
        if (user != null) {
            tvUserName.setText(user.getFullName());
            tvRole.setText(user.getRole().toUpperCase());
        }

        // Set mess info
        if (mess != null) {
            tvMessName.setText(mess.getMessName());
        }

        // Set stats
        tvMemberCount.setText(String.valueOf(memberCount));
        tvCurrentMealRate.setText(String.format(Locale.getDefault(), "৳ %.2f", mealRate));
        tvTotalMeals.setText(String.valueOf(totalMeals));
        // Show total expense (meal expense + shared expense)
        tvMonthlyExpenses.setText(String.format(Locale.getDefault(), "৳ %.2f", totalExpense));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadDashboardData();
    }
}
