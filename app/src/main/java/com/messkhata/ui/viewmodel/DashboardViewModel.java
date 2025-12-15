package com.messkhata.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.messkhata.data.model.Expense;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.data.model.Notification;
import com.messkhata.data.repository.ExpenseRepository;
import com.messkhata.data.repository.MealRepository;
import com.messkhata.data.repository.NotificationRepository;
import com.messkhata.data.repository.ReportRepository;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * ViewModel for the main dashboard/home screen.
 * Aggregates data from multiple repositories for overview display.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final MealRepository mealRepository;
    private final ExpenseRepository expenseRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final PreferenceManager prefManager;

    private final MutableLiveData<DashboardData> dashboardData = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        mealRepository = new MealRepository(application);
        expenseRepository = new ExpenseRepository(application);
        reportRepository = new ReportRepository(application);
        notificationRepository = new NotificationRepository(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get today's meals for all members.
     */
    public LiveData<List<Meal>> getTodayMeals() {
        String messId = prefManager.getMessId();
        return mealRepository.getMealsByMessAndDate(messId, DateUtils.getTodayStart());
    }

    /**
     * Get recent expenses.
     */
    public LiveData<List<Expense>> getRecentExpenses() {
        return expenseRepository.getRecentExpenses(5);
    }

    /**
     * Get current month report.
     */
    public LiveData<MonthlyReport> getCurrentMonthReport() {
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();
        return reportRepository.getReportByMonth(year, month);
    }

    /**
     * Get unread notification count.
     */
    public LiveData<Integer> getUnreadNotificationCount() {
        return notificationRepository.getUnreadCount();
    }

    /**
     * Get unread notifications.
     */
    public LiveData<List<Notification>> getUnreadNotifications() {
        return notificationRepository.getUnreadNotifications();
    }

    /**
     * Load dashboard data.
     */
    public void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String messId = prefManager.getMessId();
            int year = DateUtils.getCurrentYear();
            int month = DateUtils.getCurrentMonth();
            long startOfMonth = DateUtils.getStartOfMonth(year, month);
            long today = DateUtils.getTodayStart();

            // Get counts
            int todayMealsCount = mealRepository.getTotalMealCountInRange(messId, today, today + 86400000);
            double currentMealRate = reportRepository.getCurrentMonthMealRate();
            double totalExpensesThisMonth = expenseRepository.getTotalMealExpensesInRange(startOfMonth, System.currentTimeMillis());
            int totalMealsThisMonth = mealRepository.getTotalMealCountInRange(messId, startOfMonth, System.currentTimeMillis());

            DashboardData data = new DashboardData(
                    todayMealsCount,
                    currentMealRate,
                    totalExpensesThisMonth,
                    totalMealsThisMonth
            );

            dashboardData.postValue(data);
        });
    }

    /**
     * Get aggregated dashboard data.
     */
    public LiveData<DashboardData> getDashboardData() {
        return dashboardData;
    }

    /**
     * Get user name.
     */
    public String getUserName() {
        return prefManager.getUserName();
    }

    /**
     * Get user role.
     */
    public String getUserRole() {
        return prefManager.getUserRole();
    }

    /**
     * Refresh all dashboard data.
     */
    public void refresh() {
        loadDashboardData();
        // Also trigger report generation for current month
        reportRepository.generateReport(DateUtils.getCurrentYear(), DateUtils.getCurrentMonth());
    }

    /**
     * Dashboard data wrapper class.
     */
    public static class DashboardData {
        public final int todayMealsCount;
        public final double currentMealRate;
        public final double totalExpensesThisMonth;
        public final int totalMealsThisMonth;

        public DashboardData(int todayMealsCount, double currentMealRate,
                            double totalExpensesThisMonth, int totalMealsThisMonth) {
            this.todayMealsCount = todayMealsCount;
            this.currentMealRate = currentMealRate;
            this.totalExpensesThisMonth = totalExpensesThisMonth;
            this.totalMealsThisMonth = totalMealsThisMonth;
        }
    }
}
