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
    private TextView tvEstimatedBill;
    private TextView tvTotalExpenses;
    private ImageView ivSync;

    // DAOs
    private UserDao userDao;
    private MessDao messDao;
    private MealDao mealDao;
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
        tvEstimatedBill = view.findViewById(R.id.tvEstimatedBill);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        ivSync = view.findViewById(R.id.ivSync);
    }

    private void initializeDAOs() {
        userDao = new UserDao(requireContext());
        messDao = new MessDao(requireContext());
        mealDao = new MealDao(requireContext());
        reportDao = new ReportDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = new PreferenceManager(requireContext());
        userId = Long.parseLong(prefManager.getUserId());
        messId = Integer.parseInt(prefManager.getMessId());
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);
        ivSync.setOnClickListener(v -> loadDashboardData());
    }

    private void loadDashboardData() {
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
                
                // Get total expenses this month
                double totalExpenses = reportDao.getTotalExpenses(messId, currentMonth, currentYear);
                
                // Get member count
                List<User> members = userDao.getMembersByMessId(messId);
                int memberCount = members.size();
                
                // Calculate estimated bill
                double mealRate = mess != null ? mess.getFixedMealRate() : 50.0;
                double estimatedBill = totalMeals * mealRate;

                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    updateUI(user, mess, totalMeals, estimatedBill, totalExpenses, 
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

    private void updateUI(User user, Mess mess, int totalMeals, double estimatedBill,
                         double totalExpenses, int memberCount, double mealRate) {
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
        tvEstimatedBill.setText(String.format(Locale.getDefault(), "৳ %.2f", estimatedBill));
        tvTotalExpenses.setText(String.format(Locale.getDefault(), "৳ %.2f", totalExpenses));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadDashboardData();
    }
}
