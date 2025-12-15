package com.messkhata.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.messkhata.R;
import com.messkhata.data.model.Expense;
import com.messkhata.ui.activity.AddExpenseActivity;
import com.messkhata.ui.adapter.ExpenseAdapter;
import com.messkhata.ui.viewmodel.ExpenseViewModel;

/**
 * Fragment for expense management.
 */
public class ExpenseFragment extends Fragment {

    private ExpenseViewModel viewModel;

    private ChipGroup chipGroupCategories;
    private RecyclerView rvExpenses;
    private FloatingActionButton fabAddExpense;
    private SwipeRefreshLayout swipeRefresh;

    private ExpenseAdapter expenseAdapter;
    private String selectedCategory = null; // null means all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        initViews(view);
        setupCategoryChips();
        setupRecyclerView();
        setupSwipeRefresh();
        observeData();
    }

    private void initViews(View view) {
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        rvExpenses = view.findViewById(R.id.rvExpenses);
        fabAddExpense = view.findViewById(R.id.fabAddExpense);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        fabAddExpense.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), AddExpenseActivity.class));
        });
    }

    private void setupCategoryChips() {
        // Add "All" chip
        Chip chipAll = createChip("All", true);
        chipGroupCategories.addView(chipAll);

        // Add category chips
        for (String category : viewModel.getCategories()) {
            Chip chip = createChip(category, false);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                selectedCategory = null;
            } else {
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    String text = chip.getText().toString();
                    selectedCategory = "All".equals(text) ? null : text;
                }
            }
            loadExpenses();
        });
    }

    private Chip createChip(String text, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        return chip;
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter(new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(Expense expense) {
                showExpenseDetails(expense);
            }

            @Override
            public void onEditClick(Expense expense) {
                if (viewModel.canEditExpense(expense)) {
                    // Open edit activity
                    // TODO: Implement edit activity
                }
            }

            @Override
            public void onDeleteClick(Expense expense) {
                if (viewModel.canEditExpense(expense)) {
                    confirmDelete(expense);
                }
            }
        }, true);

        rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpenses.setAdapter(expenseAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadExpenses);
    }

    private void observeData() {
        loadExpenses();
    }

    private void loadExpenses() {
        if (selectedCategory == null) {
            viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
                swipeRefresh.setRefreshing(false);
                expenseAdapter.submitList(expenses);
            });
        } else {
            viewModel.getExpensesByCategory(selectedCategory).observe(getViewLifecycleOwner(), expenses -> {
                swipeRefresh.setRefreshing(false);
                expenseAdapter.submitList(expenses);
            });
        }
    }

    private void showExpenseDetails(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(expense.getDescription())
                .setMessage(String.format(
                        "Category: %s\nAmount: ৳%.2f\nPaid by: %s\n\nIncluded in meal rate: %s\nShared equally: %s",
                        expense.getCategory(),
                        expense.getAmount(),
                        expense.getPaidByName(),
                        expense.isIncludedInMealRate() ? "Yes" : "No",
                        expense.isSharedEqually() ? "Yes" : "No"
                ))
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmDelete(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteExpense(expense);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }
}
