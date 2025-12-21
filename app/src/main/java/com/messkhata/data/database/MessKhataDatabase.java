package com.messkhata.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessKhataDatabase extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "MessManager.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_USERS = "Users";
    public static final String TABLE_MESS = "Mess";
    public static final String TABLE_EXPENSES = "Expenses";
    public static final String TABLE_MEALS = "Meals";
    public static final String TABLE_MEAL_PREFERENCES = "MealPreferences";
    public static final String TABLE_MONTHLY_STATS = "MessMonthlyStats";
    public static final String TABLE_MONTHLY_BILLS = "MonthlyBills";
    public static final String TABLE_PAYMENTS = "Payments";

    // Singleton instance
    private static MessKhataDatabase instance;

    // Thread pool for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Private constructor
    private MessKhataDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Get singleton instance
    public static synchronized MessKhataDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new MessKhataDatabase(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables
        db.execSQL(CREATE_MESS_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_EXPENSES_TABLE);
        db.execSQL(CREATE_MEALS_TABLE);
        db.execSQL(CREATE_MEAL_PREFERENCES_TABLE);
        db.execSQL(CREATE_MONTHLY_STATS_TABLE);
        db.execSQL(CREATE_MONTHLY_BILLS_TABLE);
        db.execSQL(CREATE_PAYMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONTHLY_BILLS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONTHLY_STATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEAL_PREFERENCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESS);

        // Create new tables
        onCreate(db);
    }

    // Clear all tables (for logout)
    public void clearAllTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_PAYMENTS);
        db.execSQL("DELETE FROM " + TABLE_MONTHLY_BILLS);
        db.execSQL("DELETE FROM " + TABLE_MONTHLY_STATS);
        db.execSQL("DELETE FROM " + TABLE_MEAL_PREFERENCES);
        db.execSQL("DELETE FROM " + TABLE_MEALS);
        db.execSQL("DELETE FROM " + TABLE_EXPENSES);
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.execSQL("DELETE FROM " + TABLE_MESS);
    }

    // SQL for creating Mess table
    private static final String CREATE_MESS_TABLE =
            "CREATE TABLE " + TABLE_MESS + " (" +
                    "messId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "messName TEXT NOT NULL, " +
                    "groceryBudgetPerMeal REAL NOT NULL DEFAULT 40.00, " +
                    "cookingChargePerMeal REAL NOT NULL DEFAULT 10.00, " +
                    "createdDate INTEGER DEFAULT (strftime('%s', 'now')))");

    // SQL for creating Users table
    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    "userId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "fullName TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "phoneNumber TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "messId INTEGER, " +
                    "role TEXT DEFAULT 'member', " +
                    "joinedDate INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "isActive INTEGER DEFAULT 1, " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE SET NULL)";

    // SQL for creating Expenses table
    private static final String CREATE_EXPENSES_TABLE =
            "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    "expenseId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "messId INTEGER NOT NULL, " +
                    "addedBy INTEGER NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "description TEXT, " +
                    "expenseDate INTEGER NOT NULL, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "updatedAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (addedBy) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE)";

    // SQL for creating Meals table
    private static final String CREATE_MEALS_TABLE =
            "CREATE TABLE " + TABLE_MEALS + " (" +
                    "mealId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER NOT NULL, " +
                    "messId INTEGER NOT NULL, " +
                    "mealDate INTEGER NOT NULL, " +
                    "breakfast INTEGER DEFAULT 1, " +
                    "lunch INTEGER DEFAULT 1, " +
                    "dinner INTEGER DEFAULT 1, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "updatedAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                    "UNIQUE(userId, mealDate))";

    // SQL for creating MonthlyStats table
    private static final String CREATE_MONTHLY_STATS_TABLE =
            "CREATE TABLE " + TABLE_MONTHLY_STATS + " (" +
                    "statsId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "messId INTEGER NOT NULL, " +
                    "month INTEGER NOT NULL, " +
                    "year INTEGER NOT NULL, " +
                    "totalGrocery REAL DEFAULT 0.00, " +
                    "totalUtilities REAL DEFAULT 0.00, " +
                    "totalCleaning REAL DEFAULT 0.00, " +
                    "totalGas REAL DEFAULT 0.00, " +
                    "totalRent REAL DEFAULT 0.00, " +
                    "totalMiscellaneous REAL DEFAULT 0.00, " +
                    "totalMeals INTEGER DEFAULT 0, " +
                    "numberOfMembers INTEGER NOT NULL, " +
                    "cookingCharge REAL NOT NULL, " +
                    "isFinalized INTEGER DEFAULT 0, " +
                    "finalizedDate INTEGER, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                    "UNIQUE(messId, month, year)");

    // SQL for creating MonthlyBills table
    private static final String CREATE_MONTHLY_BILLS_TABLE =
            "CREATE TABLE " + TABLE_MONTHLY_BILLS + " (" +
                    "billId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER NOT NULL, " +
                    "messId INTEGER NOT NULL, " +
                    "month INTEGER NOT NULL, " +
                    "year INTEGER NOT NULL, " +
                    "totalMeals INTEGER NOT NULL, " +
                    "mealRate REAL NOT NULL, " +
                    "utilitiesShare REAL DEFAULT 0.00, " +
                    "cleaningShare REAL DEFAULT 0.00, " +
                    "gasShare REAL DEFAULT 0.00, " +
                    "rentShare REAL DEFAULT 0.00, " +
                    "miscShare REAL DEFAULT 0.00, " +
                    "totalPaid REAL DEFAULT 0.00, " +
                    "status TEXT DEFAULT 'pending', " +
                    "finalizedDate INTEGER, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "updatedAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                    "UNIQUE(userId, messId, month, year)");

    // SQL for creating Payments table
    private static final String CREATE_PAYMENTS_TABLE =
            "CREATE TABLE " + TABLE_PAYMENTS + " (" +
                    "paymentId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "billId INTEGER NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "messId INTEGER NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "paidDate INTEGER NOT NULL, " +
                    "addedBy INTEGER NOT NULL, " +
                    "paymentMethod TEXT DEFAULT 'Cash', " +
                    "notes TEXT, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (billId) REFERENCES " + TABLE_MONTHLY_BILLS + "(billId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (addedBy) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE)";

    // SQL for creating MealPreferences table
    private static final String CREATE_MEAL_PREFERENCES_TABLE =
            "CREATE TABLE " + TABLE_MEAL_PREFERENCES + " (" +
                    "preferenceId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER NOT NULL, " +
                    "messId INTEGER NOT NULL, " +
                    "breakfast INTEGER DEFAULT 1, " +
                    "lunch INTEGER DEFAULT 1, " +
                    "dinner INTEGER DEFAULT 1, " +
                    "effectiveFrom INTEGER NOT NULL, " +
                    "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE)";
}