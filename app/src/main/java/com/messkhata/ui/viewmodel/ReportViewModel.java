package com.messkhata.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.data.repository.ReportRepository;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * ViewModel for Reports and Balance screens.
 */
public class ReportViewModel extends AndroidViewModel {

    private final ReportRepository reportRepository;
    private final PreferenceManager prefManager;
    private final MutableLiveData<Double> currentMealRate = new MutableLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        reportRepository = new ReportRepository(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get report for a specific month.
     */
    public LiveData<MonthlyReport> getReportByMonth(int year, int month) {
        return reportRepository.getReportByMonth(year, month);
    }

    /**
     * Get current month's report.
     */
    public LiveData<MonthlyReport> getCurrentMonthReport() {
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();
        return reportRepository.getReportByMonth(year, month);
    }

    /**
     * Get all reports.
     */
    public LiveData<List<MonthlyReport>> getAllReports() {
        return reportRepository.getAllReports();
    }

    /**
     * Get member balances for a report.
     */
    public LiveData<List<MemberBalance>> getBalancesByReport(String reportId) {
        return reportRepository.getBalancesByReport(reportId);
    }

    /**
     * Get current user's balance history.
     */
    public LiveData<List<MemberBalance>> getMyBalanceHistory() {
        String userId = prefManager.getUserId();
        return reportRepository.getUserBalanceHistory(userId);
    }

    /**
     * Get current user's balance for current month.
     */
    public LiveData<MemberBalance> getMyCurrentBalance() {
        String userId = prefManager.getUserId();
        return reportRepository.getCurrentMonthBalance(userId);
    }

    /**
     * Generate report for a specific month.
     */
    public void generateReport(int year, int month) {
        reportRepository.generateReport(year, month);
    }

    /**
     * Generate current month's report.
     */
    public void generateCurrentMonthReport() {
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();
        reportRepository.generateReport(year, month);
    }

    /**
     * Finalize a report (Admin only).
     */
    public void finalizeReport(String reportId) {
        reportRepository.finalizeReport(reportId);
    }

    /**
     * Get current meal rate (live estimate).
     */
    public LiveData<Double> getCurrentMealRate() {
        refreshCurrentMealRate();
        return currentMealRate;
    }

    /**
     * Refresh current meal rate.
     */
    public void refreshCurrentMealRate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            double rate = reportRepository.getCurrentMonthMealRate();
            currentMealRate.postValue(rate);
        });
    }

    /**
     * Check if current user is admin.
     */
    public boolean isAdmin() {
        return prefManager.isAdmin();
    }

    /**
     * Format currency.
     */
    public String formatCurrency(double amount) {
        return String.format("৳%.2f", amount);
    }
}
