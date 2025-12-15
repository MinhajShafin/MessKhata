package com.messkhata.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Expense;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.utils.Constants;
import com.messkhata.utils.IdGenerator;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * Repository for Expense data operations.
 */
public class ExpenseRepository {

    private final ExpenseDao expenseDao;
    private final FirebaseSyncManager syncManager;
    private final NetworkUtils networkUtils;
    private final PreferenceManager prefManager;

    public ExpenseRepository(Application application) {
        MessKhataDatabase database = MessKhataDatabase.getInstance(application);
        expenseDao = database.expenseDao();
        syncManager = FirebaseSyncManager.getInstance(database);
        networkUtils = NetworkUtils.getInstance(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get all expenses for the current mess.
     */
    public LiveData<List<Expense>> getAllExpenses() {
        String messId = prefManager.getMessId();
        return expenseDao.getExpensesByMess(messId);
    }

    /**
     * Get expenses by category.
     */
    public LiveData<List<Expense>> getExpensesByCategory(String category) {
        String messId = prefManager.getMessId();
        return expenseDao.getExpensesByCategory(messId, category);
    }

    /**
     * Get expenses in a date range.
     */
    public LiveData<List<Expense>> getExpensesByDateRange(long startDate, long endDate) {
        String messId = prefManager.getMessId();
        return expenseDao.getExpensesByDateRange(messId, startDate, endDate);
    }

    /**
     * Get recent expenses.
     */
    public LiveData<List<Expense>> getRecentExpenses(int limit) {
        String messId = prefManager.getMessId();
        return expenseDao.getRecentExpenses(messId, limit);
    }

    /**
     * Add a new expense.
     */
    public void addExpense(String category, String description, double amount,
                           long date, boolean isSharedEqually, boolean isIncludedInMealRate) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String messId = prefManager.getMessId();
            String userId = prefManager.getUserId();
            String userName = prefManager.getUserName();

            String expenseId = IdGenerator.generateId();
            Expense expense = new Expense(expenseId, messId, category, description,
                                         amount, date, userId, userName);
            expense.setSharedEqually(isSharedEqually);
            expense.setIncludedInMealRate(isIncludedInMealRate);

            expenseDao.insert(expense);

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Update an expense.
     */
    public void updateExpense(Expense expense) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            expense.setUpdatedAt(System.currentTimeMillis());
            expense.setSynced(false);
            expense.setPendingAction(Constants.ACTION_UPDATE);
            expenseDao.update(expense);

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Delete an expense.
     */
    public void deleteExpense(Expense expense) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            // Check permission
            if (!canModifyExpense(expense)) {
                return; // Silently fail if no permission
            }

            expense.setSynced(false);
            expense.setPendingAction(Constants.ACTION_DELETE);
            expense.setUpdatedAt(System.currentTimeMillis());
            expenseDao.update(expense);

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Check if current user can modify this expense.
     * Only the user who created the expense, managers, or admins can modify.
     */
    private boolean canModifyExpense(Expense expense) {
        String currentUserId = prefManager.getUserId();
        String currentRole = prefManager.getUserRole();

        // Admins and managers can modify any expense
        if (Constants.ROLE_ADMIN.equals(currentRole) ||
            Constants.ROLE_MANAGER.equals(currentRole)) {
            return true;
        }

        // Members can only modify their own expenses
        return expense.getPaidById().equals(currentUserId);
    }

    /**
     * Get total meal expenses in a date range.
     */
    public double getTotalMealExpensesInRange(long startDate, long endDate) {
        String messId = prefManager.getMessId();
        return expenseDao.getTotalMealExpensesInRange(messId, startDate, endDate);
    }

    /**
     * Get total fixed expenses in a date range.
     */
    public double getTotalFixedExpensesInRange(long startDate, long endDate) {
        String messId = prefManager.getMessId();
        return expenseDao.getTotalFixedExpensesInRange(messId, startDate, endDate);
    }

    /**
     * Get total paid by a user in a date range.
     */
    public double getTotalPaidByUserInRange(String userId, long startDate, long endDate) {
        String messId = prefManager.getMessId();
        return expenseDao.getTotalPaidByUserInRange(messId, userId, startDate, endDate);
    }
}
