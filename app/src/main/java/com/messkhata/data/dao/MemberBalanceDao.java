package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.MemberBalance;

import java.util.List;

@Dao
public interface MemberBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MemberBalance balance);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MemberBalance> balances);

    @Update
    void update(MemberBalance balance);

    @Delete
    void delete(MemberBalance balance);

    @Query("SELECT * FROM member_balances WHERE id = :balanceId")
    LiveData<MemberBalance> getBalanceById(String balanceId);

    @Query("SELECT * FROM member_balances WHERE id = :balanceId")
    MemberBalance getBalanceByIdSync(String balanceId);

    @Query("SELECT * FROM member_balances WHERE messId = :messId AND userId = :userId AND year = :year AND month = :month LIMIT 1")
    LiveData<MemberBalance> getBalanceByUserAndMonth(String messId, String userId, int year, int month);

    @Query("SELECT * FROM member_balances WHERE messId = :messId AND userId = :userId AND year = :year AND month = :month LIMIT 1")
    MemberBalance getBalanceByUserAndMonthSync(String messId, String userId, int year, int month);

    @Query("SELECT * FROM member_balances WHERE reportId = :reportId ORDER BY userName")
    LiveData<List<MemberBalance>> getBalancesByReport(String reportId);

    @Query("SELECT * FROM member_balances WHERE reportId = :reportId")
    List<MemberBalance> getBalancesByReportSync(String reportId);

    @Query("SELECT * FROM member_balances WHERE userId = :userId ORDER BY year DESC, month DESC")
    LiveData<List<MemberBalance>> getBalancesByUser(String userId);

    // Get members who owe money (negative balance)
    @Query("SELECT * FROM member_balances WHERE reportId = :reportId AND balance < 0 ORDER BY balance ASC")
    LiveData<List<MemberBalance>> getMembersWholeOwe(String reportId);

    // Get members who are owed money (positive balance)
    @Query("SELECT * FROM member_balances WHERE reportId = :reportId AND balance > 0 ORDER BY balance DESC")
    LiveData<List<MemberBalance>> getMembersWhoAreOwed(String reportId);

    // Get unsynced balances
    @Query("SELECT * FROM member_balances WHERE isSynced = 0")
    List<MemberBalance> getUnsyncedBalances();

    @Query("UPDATE member_balances SET isSynced = 1, pendingAction = NULL WHERE id = :balanceId")
    void markAsSynced(String balanceId);

    @Query("DELETE FROM member_balances WHERE id = :balanceId")
    void deleteById(String balanceId);

    @Query("DELETE FROM member_balances WHERE reportId = :reportId")
    void deleteByReportId(String reportId);
}
