package com.messkhata.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.dao.MealDao;
import com.messkhata.data.dao.MemberBalanceDao;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.MonthlyReportDao;
import com.messkhata.data.dao.NotificationDao;
import com.messkhata.data.dao.SyncMetadataDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.model.Expense;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.data.model.Notification;
import com.messkhata.data.model.SyncMetadata;
import com.messkhata.data.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        User.class,
        Mess.class,
        Meal.class,
        Expense.class,
        MonthlyReport.class,
        MemberBalance.class,
        Notification.class,
        SyncMetadata.class
    },
    version = 1,
    exportSchema = false
)
public abstract class MessKhataDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "messkhata_database";
    private static volatile MessKhataDatabase INSTANCE;

    // Thread pool for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Abstract methods to get DAOs
    public abstract UserDao userDao();
    public abstract MessDao messDao();
    public abstract MealDao mealDao();
    public abstract ExpenseDao expenseDao();
    public abstract MonthlyReportDao monthlyReportDao();
    public abstract MemberBalanceDao memberBalanceDao();
    public abstract NotificationDao notificationDao();
    public abstract SyncMetadataDao syncMetadataDao();

    // Singleton pattern
    public static MessKhataDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MessKhataDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MessKhataDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // Clear all tables (for logout)
    public void clearAllTables() {
        databaseWriteExecutor.execute(() -> {
            clearAllTables();
        });
    }
}
