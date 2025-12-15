package com.messkhata.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MemberBalanceDao;
import com.messkhata.data.dao.MonthlyReportDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.data.model.User;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.utils.Constants;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.IdGenerator;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * Repository for Monthly Reports and Balance calculations.
 * Handles dynamic meal rate and individual dues calculation.
 */
public class ReportRepository {

    private final MonthlyReportDao reportDao;
    private final MemberBalanceDao balanceDao;
    private final MealDao mealDao;
    private final ExpenseDao expenseDao;
    private final UserDao userDao;
    private final FirebaseSyncManager syncManager;
    private final NetworkUtils networkUtils;
    private final PreferenceManager prefManager;

    public ReportRepository(Application application) {
        MessKhataDatabase database = MessKhataDatabase.getInstance(application);
        reportDao = database.monthlyReportDao();
        balanceDao = database.memberBalanceDao();
        mealDao = database.mealDao();
        expenseDao = database.expenseDao();
        userDao = database.userDao();
        syncManager = FirebaseSyncManager.getInstance(database);
        networkUtils = NetworkUtils.getInstance(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get report for a specific month.
     */
    public LiveData<MonthlyReport> getReportByMonth(int year, int month) {
        String messId = prefManager.getMessId();
        return reportDao.getReportByMonth(messId, year, month);
    }

    /**
     * Get all reports for the mess.
     */
    public LiveData<List<MonthlyReport>> getAllReports() {
        String messId = prefManager.getMessId();
        return reportDao.getAllReportsByMess(messId);
    }

    /**
     * Get member balances for a report.
     */
    public LiveData<List<MemberBalance>> getBalancesByReport(String reportId) {
        return balanceDao.getBalancesByReport(reportId);
    }

    /**
     * Get balance history for a user.
     */
    public LiveData<List<MemberBalance>> getUserBalanceHistory(String userId) {
        return balanceDao.getBalancesByUser(userId);
    }

    /**
     * Generate or update monthly report.
     * This calculates meal rate and individual balances dynamically.
     */
    public void generateReport(int year, int month) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String messId = prefManager.getMessId();

            // Get date range
            long startDate = DateUtils.getStartOfMonth(year, month);
            long endDate = DateUtils.getEndOfMonth(year, month);

            // Calculate totals
            int totalMeals = mealDao.getTotalMealCountInRange(messId, startDate, endDate);
            double totalMealExpenses = expenseDao.getTotalMealExpensesInRange(
                    messId, startDate, endDate);
            double totalFixedExpenses = expenseDao.getTotalFixedExpensesInRange(
                    messId, startDate, endDate);

            // Calculate meal rate
            double mealRate = totalMeals > 0 ? totalMealExpenses / totalMeals : 0;

            // Get or create report
            String reportId = IdGenerator.generateReportId(messId, year, month);
            MonthlyReport report = reportDao.getReportByMonthSync(messId, year, month);

            if (report == null) {
                report = new MonthlyReport(reportId, messId, year, month);
            }

            report.setTotalMeals(totalMeals);
            report.setTotalMealExpenses(totalMealExpenses);
            report.setTotalFixedExpenses(totalFixedExpenses);
            report.setMealRate(mealRate);
            report.setUpdatedAt(System.currentTimeMillis());
            report.setSynced(false);
            report.setPendingAction(Constants.ACTION_UPDATE);

            reportDao.insert(report);

            // Calculate individual balances
            calculateMemberBalances(reportId, messId, year, month,
                                   startDate, endDate, mealRate, totalFixedExpenses);

            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }
        });
    }

    /**
     * Calculate balance for each member.
     */
    private void calculateMemberBalances(String reportId, String messId, int year, int month,
                                         long startDate, long endDate,
                                         double mealRate, double totalFixedExpenses) {
        // Get all active members
        List<User> members = userDao.getActiveUsersByMessSync(messId);
        int memberCount = members.size();

        // Calculate fixed expense per member
        double fixedExpensePerMember = memberCount > 0 ? totalFixedExpenses / memberCount : 0;

        for (User member : members) {
            String balanceId = IdGenerator.generateBalanceId(messId, member.getId(), year, month);

            // Get or create balance
            MemberBalance balance = balanceDao.getBalanceByUserAndMonthSync(
                    messId, member.getId(), year, month);

            if (balance == null) {
                balance = new MemberBalance(balanceId, messId, member.getId(),
                                           member.getName(), reportId, year, month);
            }

            // Calculate member's meals
            int memberMeals = mealDao.getUserMealCountInRange(
                    member.getId(), messId, startDate, endDate);

            // Calculate member's total paid
            double totalPaid = expenseDao.getTotalPaidByUserInRange(
                    messId, member.getId(), startDate, endDate);

            balance.setTotalMeals(memberMeals);
            balance.setTotalPaid(totalPaid);
            balance.calculateBalance(mealRate, fixedExpensePerMember);
            balance.setUpdatedAt(System.currentTimeMillis());
            balance.setSynced(false);
            balance.setPendingAction(Constants.ACTION_UPDATE);

            balanceDao.insert(balance);
        }
    }

    /**
     * Finalize a report (prevents further modifications).
     * Only admins can finalize.
     */
    public void finalizeReport(String reportId) {
        if (!prefManager.isAdmin()) {
            return;
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            MonthlyReport report = reportDao.getReportByIdSync(reportId);
            if (report != null) {
                report.setFinalized(true);
                report.setUpdatedAt(System.currentTimeMillis());
                report.setSynced(false);
                report.setPendingAction(Constants.ACTION_UPDATE);
                reportDao.update(report);

                if (networkUtils.isOnline()) {
                    syncManager.uploadPendingChanges();
                }
            }
        });
    }

    /**
     * Get current month's estimated meal rate (for live display).
     */
    public double getCurrentMonthMealRate() {
        String messId = prefManager.getMessId();
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();

        long startDate = DateUtils.getStartOfMonth(year, month);
        long endDate = System.currentTimeMillis(); // Up to now

        int totalMeals = mealDao.getTotalMealCountInRange(messId, startDate, endDate);
        double totalMealExpenses = expenseDao.getTotalMealExpensesInRange(
                messId, startDate, endDate);

        return totalMeals > 0 ? totalMealExpenses / totalMeals : 0;
    }

    /**
     * Get member's current month balance.
     */
    public LiveData<MemberBalance> getCurrentMonthBalance(String userId) {
        String messId = prefManager.getMessId();
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();

        return balanceDao.getBalanceByUserAndMonth(messId, userId, year, month);
    }
}
