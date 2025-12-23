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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.messkhata.R;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Expense;
import com.messkhata.data.sync.RealtimeSyncManager;
import com.messkhata.ui.activity.AddExpenseActivity;
import com.messkhata.ui.adapter.ExpenseAdapter;
import com.messkhata.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Expense Fragment - View and manage expenses
 */
public class ExpenseFragment extends Fragment implements ExpenseAdapter.OnExpenseActionListener {

    // UI Components
    private TextView tvCurrentMonth;
    private TextView tvTotalAmount;
    private TextView tvExpenseCount;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private RecyclerView rvExpenseList;
    private FloatingActionButton fabAddExpense;

    // Adapter
    private ExpenseAdapter expenseAdapter;

    // DAOs
    private ExpenseDao expenseDao;

    // Session data
    private PreferenceManager prefManager;
    private int messId;
    private String userRole;

    // Current month
    private Calendar currentMonth;

    // Expense list
    private List<Expense> expenseList = new ArrayList<>();

    // Broadcast receiver for real-time updates
    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Data updated from cloud - refresh UI
            if (isAdded() && getActivity() != null) {
                loadExpenses();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAO();
        loadSessionData();
        setupListeners();
        loadExpenses();
    }

    private void initializeViews(View view) {
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvExpenseCount = view.findViewById(R.id.tvExpenseCount);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvExpenseList = view.findViewById(R.id.rvExpenses);
        fabAddExpense = view.findViewById(R.id.fabAddExpense);

        // Setup RecyclerView
        rvExpenseList.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void initializeDAO() {
        expenseDao = new ExpenseDao(requireContext());
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
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        expenseAdapter = new ExpenseAdapter(expenseList, isAdmin, this);
        rvExpenseList.setAdapter(expenseAdapter);
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadExpenses();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadExpenses();
        });

        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddExpenseActivity.class);
            startActivity(intent);
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void loadExpenses() {
        // Check if views are initialized
        if (tvTotalAmount == null || tvExpenseCount == null || expenseAdapter == null) {
            return;
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int year = currentMonth.get(Calendar.YEAR);
                int month = currentMonth.get(Calendar.MONTH) + 1;

                List<Expense> expenses = expenseDao.getExpensesByMonth(messId, month, year);
                double totalAmount = 0;
                if (expenses != null) {
                    for (Expense expense : expenses) {
                        totalAmount += expense.getAmount();
                    }
                }

                double finalTotalAmount = totalAmount;
                requireActivity().runOnUiThread(() -> {
                    expenseList.clear();
                    if (expenses != null) {
                        expenseList.addAll(expenses);
                    }

                    tvTotalAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", finalTotalAmount));
                    tvExpenseCount.setText(String.valueOf(expenseList.size()));

                    // Update adapter
                    expenseAdapter.updateExpenses(expenseList);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Error loading expenses",
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
        filter.addAction(RealtimeSyncManager.ACTION_EXPENSES_UPDATED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(syncReceiver, filter);

        loadExpenses();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(syncReceiver);
    }

    @Override
    public void onEditExpense(Expense expense) {
        // TODO: Implement edit expense functionality
        Toast.makeText(requireContext(), "Edit expense: " + expense.getDescription(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteExpense(Expense expense) {
        // TODO: Implement delete expense functionality
        Toast.makeText(requireContext(), "Delete expense: " + expense.getDescription(), Toast.LENGTH_SHORT).show();
    }
}
