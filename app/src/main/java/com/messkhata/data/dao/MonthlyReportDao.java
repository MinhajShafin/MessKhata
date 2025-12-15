package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.MonthlyReport;

import java.util.List;

@Dao
public interface MonthlyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MonthlyReport report);

    @Update
    void update(MonthlyReport report);

    @Delete
    void delete(MonthlyReport report);

    @Query("SELECT * FROM monthly_reports WHERE id = :reportId")
    LiveData<MonthlyReport> getReportById(String reportId);

    @Query("SELECT * FROM monthly_reports WHERE id = :reportId")
    MonthlyReport getReportByIdSync(String reportId);

    @Query("SELECT * FROM monthly_reports WHERE messId = :messId AND year = :year AND month = :month LIMIT 1")
    LiveData<MonthlyReport> getReportByMonth(String messId, int year, int month);

    @Query("SELECT * FROM monthly_reports WHERE messId = :messId AND year = :year AND month = :month LIMIT 1")
    MonthlyReport getReportByMonthSync(String messId, int year, int month);

    @Query("SELECT * FROM monthly_reports WHERE messId = :messId ORDER BY year DESC, month DESC")
    LiveData<List<MonthlyReport>> getAllReportsByMess(String messId);

    @Query("SELECT * FROM monthly_reports WHERE messId = :messId AND year = :year ORDER BY month DESC")
    LiveData<List<MonthlyReport>> getReportsByYear(String messId, int year);

    // Get unsynced reports
    @Query("SELECT * FROM monthly_reports WHERE isSynced = 0")
    List<MonthlyReport> getUnsyncedReports();

    @Query("UPDATE monthly_reports SET isSynced = 1, pendingAction = NULL WHERE id = :reportId")
    void markAsSynced(String reportId);

    @Query("DELETE FROM monthly_reports WHERE id = :reportId")
    void deleteById(String reportId);
}
