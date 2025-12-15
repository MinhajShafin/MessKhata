package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Expense> expenses);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    LiveData<Expense> getExpenseById(String expenseId);

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    Expense getExpenseByIdSync(String expenseId);

    // Get all expenses for a mess
    @Query("SELECT * FROM expenses WHERE messId = :messId ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByMess(String messId);

    // Get expenses by category
    @Query("SELECT * FROM expenses WHERE messId = :messId AND category = :category ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByCategory(String messId, String category);

    // Get expenses in a date range
    @Query("SELECT * FROM expenses WHERE messId = :messId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByDateRange(String messId, long startDate, long endDate);

    @Query("SELECT * FROM expenses WHERE messId = :messId AND date >= :startDate AND date <= :endDate")
    List<Expense> getExpensesByDateRangeSync(String messId, long startDate, long endDate);

    // Get total expenses for meal rate calculation
    @Query("SELECT SUM(amount) FROM expenses WHERE messId = :messId AND date >= :startDate AND date <= :endDate AND isIncludedInMealRate = 1")
    double getTotalMealExpensesInRange(String messId, long startDate, long endDate);

    // Get total fixed expenses (shared equally)
    @Query("SELECT SUM(amount) FROM expenses WHERE messId = :messId AND date >= :startDate AND date <= :endDate AND isIncludedInMealRate = 0")
    double getTotalFixedExpensesInRange(String messId, long startDate, long endDate);

    // Get total paid by a user in date range
    @Query("SELECT SUM(amount) FROM expenses WHERE messId = :messId AND paidById = :userId AND date >= :startDate AND date <= :endDate")
    double getTotalPaidByUserInRange(String messId, String userId, long startDate, long endDate);

    // Get recent expenses
    @Query("SELECT * FROM expenses WHERE messId = :messId ORDER BY date DESC LIMIT :limit")
    LiveData<List<Expense>> getRecentExpenses(String messId, int limit);

    // Get unsynced expenses
    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    List<Expense> getUnsyncedExpenses();

    @Query("UPDATE expenses SET isSynced = 1, pendingAction = NULL WHERE id = :expenseId")
    void markAsSynced(String expenseId);

    @Query("UPDATE expenses SET pendingAction = :action, isSynced = 0, updatedAt = :timestamp WHERE id = :expenseId")
    void markForSync(String expenseId, String action, long timestamp);

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    void deleteById(String expenseId);
}
