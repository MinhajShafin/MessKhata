package com.messkhata.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messkhata.R;
import com.messkhata.data.dao.ReportDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.sync.RealtimeSyncManager;
import com.messkhata.ui.adapter.MemberBalanceAdapter;
import com.messkhata.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Report Fragment - View member balances and reports
 */
public class ReportFragment extends Fragment {

    // UI Components
    private TextView tvSelectedMonth;
    private TextView tvTotalExpenses;
    private TextView tvTotalMeals;
    private TextView tvMealRate;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private RecyclerView rvMemberBalances;

    // Expense breakdown TextViews
    private TextView tvGroceryAmount;
    private TextView tvUtilityAmount;
    private TextView tvGasAmount;
    private TextView tvRentAmount;
    private TextView tvOtherAmount;

    // Adapter
    private MemberBalanceAdapter memberBalanceAdapter;

    // DAOs
    private ReportDao reportDao;

    // Session data
    private PreferenceManager prefManager;
    private int messId;
    private String userRole;

    // Broadcast receiver for real-time updates
    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Data updated from cloud - refresh UI
            if (isAdded() && getActivity() != null) {
                loadReport();
            }
        }
    };

    // Current month
    private Calendar currentMonth;

    // Member balance list
    private List<MemberBalance> memberBalances = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAO();
        loadSessionData();
        setupListeners();
        loadReport();
    }

    private void initializeViews(View view) {
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        tvTotalMeals = view.findViewById(R.id.tvTotalMeals);
        tvMealRate = view.findViewById(R.id.tvMealRate);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvMemberBalances = view.findViewById(R.id.rvBalances);

        // Expense breakdown TextViews
        tvGroceryAmount = view.findViewById(R.id.tvGroceryAmount);
        tvUtilityAmount = view.findViewById(R.id.tvUtilityAmount);
        tvGasAmount = view.findViewById(R.id.tvGasAmount);
        tvRentAmount = view.findViewById(R.id.tvRentAmount);
        tvOtherAmount = view.findViewById(R.id.tvOtherAmount);

        // Setup RecyclerView
        rvMemberBalances.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void initializeDAO() {
        reportDao = new ReportDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = PreferenceManager.getInstance(requireContext());

        // Check if session exists
        String messIdStr = prefManager.getMessId();
        String userRoleStr = prefManager.getUserRole();

        if (messIdStr == null) {
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        messId = Integer.parseInt(messIdStr);
        userRole = userRoleStr != null ? userRoleStr : "MEMBER";
        currentMonth = Calendar.getInstance();
        updateMonthDisplay();

        // Initialize adapter
        memberBalanceAdapter = new MemberBalanceAdapter(memberBalances);
        rvMemberBalances.setAdapter(memberBalanceAdapter);
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadReport();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadReport();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvSelectedMonth.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void loadReport() {
        // Check if views are initialized
        if (tvTotalExpenses == null || tvTotalMeals == null || memberBalanceAdapter == null) {
            return;
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int year = currentMonth.get(Calendar.YEAR);
                int month = currentMonth.get(Calendar.MONTH) + 1;

                // Load member balances
                List<MemberBalance> balances = reportDao.getMemberBalances(messId, year, month);

                // Load summary data
                double totalExpenses = reportDao.getTotalExpenses(messId, month, year);
                int totalMeals = reportDao.getTotalMeals(messId, month, year);
                double mealRate = reportDao.calculateMealRate(messId, month, year);

                // Load expense breakdown by category
                double groceryAmount = reportDao.getExpenseByCategory(messId, "Grocery", month, year);
                double utilityAmount = reportDao.getExpenseByCategory(messId, "Utility", month, year);
                double gasAmount = reportDao.getExpenseByCategory(messId, "Gas", month, year);
                double rentAmount = reportDao.getExpenseByCategory(messId, "Rent", month, year);
                double maintenanceAmount = reportDao.getExpenseByCategory(messId, "Maintenance", month, year);
                double otherAmount = reportDao.getExpenseByCategory(messId, "Other", month, year);
                double totalOtherAmount = maintenanceAmount + otherAmount;

                requireActivity().runOnUiThread(() -> {
                    memberBalances.clear();
                    if (balances != null) {
                        memberBalances.addAll(balances);
                    }
                    tvTotalExpenses.setText(String.format(Locale.getDefault(), "৳ %.2f", totalExpenses));
                    tvTotalMeals.setText(String.valueOf(totalMeals));
                    tvMealRate.setText(String.format(Locale.getDefault(), "৳ %.2f", mealRate));

                    // Update expense breakdown
                    tvGroceryAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", groceryAmount));
                    tvUtilityAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", utilityAmount));
                    tvGasAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", gasAmount));
                    tvRentAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", rentAmount));
                    tvOtherAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", totalOtherAmount));

                    // Update adapter
                    memberBalanceAdapter.updateMemberBalances(memberBalances);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Error loading report",
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for real-time sync updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(RealtimeSyncManager.ACTION_DATA_UPDATED);
        filter.addAction(RealtimeSyncManager.ACTION_USERS_UPDATED);
        filter.addAction(RealtimeSyncManager.ACTION_EXPENSES_UPDATED);
        filter.addAction(RealtimeSyncManager.ACTION_MEALS_UPDATED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(syncReceiver, filter);

        loadReport();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(syncReceiver);
    }
}
