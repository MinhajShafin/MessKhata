package com.messkhata.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.messkhata.data.model.Mess;

import java.util.List;

@Dao
public interface MessDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Mess mess);

    @Update
    void update(Mess mess);

    @Delete
    void delete(Mess mess);

    @Query("SELECT * FROM messes WHERE id = :messId")
    LiveData<Mess> getMessById(String messId);

    @Query("SELECT * FROM messes WHERE id = :messId")
    Mess getMessByIdSync(String messId);

    @Query("SELECT * FROM messes WHERE joinCode = :joinCode LIMIT 1")
    Mess getMessByJoinCode(String joinCode);

    @Query("SELECT * FROM messes WHERE adminId = :adminId")
    LiveData<List<Mess>> getMessesByAdmin(String adminId);

    @Query("SELECT * FROM messes WHERE isSynced = 0")
    List<Mess> getUnsyncedMesses();

    @Query("UPDATE messes SET isSynced = 1, pendingAction = NULL WHERE id = :messId")
    void markAsSynced(String messId);

    @Query("UPDATE messes SET memberCount = :count, updatedAt = :timestamp WHERE id = :messId")
    void updateMemberCount(String messId, int count, long timestamp);

    @Query("DELETE FROM messes WHERE id = :messId")
    void deleteById(String messId);
}
