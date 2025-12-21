package com.messkhata.ui.fragment;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.messkhata.R;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Expense;
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
        messId = Integer.parseInt(prefManager.getMessId());
        userRole = prefManager.getRole();
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
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                requireActivity().runOnUiThread(() -> {
                    expenseList.clear();
                    if (expenses != null) {
                        expenseList.addAll(expenses);
                    }
                    
                    tvTotalAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", totalAmount));
                    tvExpenseCount.setText(String.valueOf(expenseList.size()));
                    
                    // Update adapter
                    expenseAdapter.updateExpenses(expenseList);
                }); // For now, showing a simple message
                    if (expenseList.isEmpty()) {
                        Toast.makeText(requireContext(), 
                            "No expenses for this month", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), 
                        "Error loading expenses", 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
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
}   }
}
