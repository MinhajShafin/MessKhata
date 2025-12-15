package com.messkhata.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.messkhata.data.model.Expense;
import com.messkhata.data.repository.ExpenseRepository;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * ViewModel for Expense management screens.
 */
public class ExpenseViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final PreferenceManager prefManager;

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get all expenses.
     */
    public LiveData<List<Expense>> getAllExpenses() {
        return expenseRepository.getAllExpenses();
    }

    /**
     * Get expenses by category.
     */
    public LiveData<List<Expense>> getExpensesByCategory(String category) {
        return expenseRepository.getExpensesByCategory(category);
    }

    /**
     * Get expenses in a date range.
     */
    public LiveData<List<Expense>> getExpensesByDateRange(long startDate, long endDate) {
        return expenseRepository.getExpensesByDateRange(startDate, endDate);
    }

    /**
     * Get recent expenses.
     */
    public LiveData<List<Expense>> getRecentExpenses() {
        return expenseRepository.getRecentExpenses(20);
    }

    /**
     * Add a new expense.
     */
    public void addExpense(String category, String description, double amount,
                           long date, boolean isSharedEqually, boolean isIncludedInMealRate) {
        expenseRepository.addExpense(category, description, amount, date,
                                     isSharedEqually, isIncludedInMealRate);
    }

    /**
     * Update an expense.
     */
    public void updateExpense(Expense expense) {
        expenseRepository.updateExpense(expense);
    }

    /**
     * Delete an expense.
     */
    public void deleteExpense(Expense expense) {
        expenseRepository.deleteExpense(expense);
    }

    /**
     * Check if current user can edit this expense.
     */
    public boolean canEditExpense(Expense expense) {
        String currentUserId = prefManager.getUserId();

        // Admins and managers can edit any expense
        if (prefManager.isAdminOrManager()) {
            return true;
        }

        // Members can only edit their own expenses
        return expense.getPaidById().equals(currentUserId);
    }

    /**
     * Get expense categories.
     */
    public String[] getCategories() {
        return new String[]{
                Expense.GROCERY,
                Expense.UTILITY,
                Expense.GAS,
                Expense.RENT,
                Expense.MAINTENANCE,
                Expense.OTHER
        };
    }
}
