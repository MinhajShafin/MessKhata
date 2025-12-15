package com.messkhata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.messkhata.R;
import com.messkhata.data.model.Expense;
import com.messkhata.ui.adapter.ExpenseAdapter;
import com.messkhata.ui.viewmodel.DashboardViewModel;

/**
 * Dashboard Fragment - Main home screen.
 */
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    private TextView tvWelcome;
    private TextView tvRole;
    private TextView tvTodayMeals;
    private TextView tvCurrentMealRate;
    private TextView tvMonthlyExpenses;
    private TextView tvMonthlyMeals;
    private RecyclerView rvRecentExpenses;
    private SwipeRefreshLayout swipeRefresh;

    private ExpenseAdapter expenseAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        observeData();

        // Load initial data
        viewModel.loadDashboardData();
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvRole = view.findViewById(R.id.tvRole);
        tvTodayMeals = view.findViewById(R.id.tvTodayMeals);
        tvCurrentMealRate = view.findViewById(R.id.tvCurrentMealRate);
        tvMonthlyExpenses = view.findViewById(R.id.tvMonthlyExpenses);
        tvMonthlyMeals = view.findViewById(R.id.tvMonthlyMeals);
        rvRecentExpenses = view.findViewById(R.id.rvRecentExpenses);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Set welcome message
        tvWelcome.setText("Hello, " + viewModel.getUserName() + "!");
        tvRole.setText(viewModel.getUserRole());
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter(new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(Expense expense) {
                // Show expense details
            }

            @Override
            public void onEditClick(Expense expense) {
                // Not shown in dashboard
            }

            @Override
            public void onDeleteClick(Expense expense) {
                // Not shown in dashboard
            }
        }, false);

        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentExpenses.setAdapter(expenseAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refresh();
        });
    }

    private void observeData() {
        // Observe dashboard data
        viewModel.getDashboardData().observe(getViewLifecycleOwner(), data -> {
            swipeRefresh.setRefreshing(false);

            tvTodayMeals.setText(String.valueOf(data.todayMealsCount));
            tvCurrentMealRate.setText(String.format("৳%.2f", data.currentMealRate));
            tvMonthlyExpenses.setText(String.format("৳%.2f", data.totalExpensesThisMonth));
            tvMonthlyMeals.setText(String.valueOf(data.totalMealsThisMonth));
        });

        // Observe recent expenses
        viewModel.getRecentExpenses().observe(getViewLifecycleOwner(), expenses -> {
            expenseAdapter.submitList(expenses);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadDashboardData();
    }
}
