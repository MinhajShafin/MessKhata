package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.messkhata.data.database.MessKhataDatabase;

/**
 * Data Access Object for User operations
 */
public class UserDao {

    private MessKhataDatabase dbHelper;

    public UserDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
    }

    /**
     * Register a new user
     * @return true if successful, false if email/phone already exists
     */
    public boolean registerUser(String name, String email, String phone, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Check if email or phone already exists
            if (isEmailExists(email) || isPhoneExists(phone)) {
                return false;
            }

            // Insert new user
            ContentValues values = new ContentValues();
            values.put("fullName", name);
            values.put("email", email);
            values.put("phoneNumber", phone);
            values.put("password", password); // Hash in production!
            values.put("role", "member");
            values.put("isActive", 1);
            values.put("joinedDate", System.currentTimeMillis() / 1000);

            long result = db.insert(MessKhataDatabase.TABLE_USERS, null, values);
            return result != -1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if email already exists
     */
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT 1 FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE email = ? LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{email});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Check if phone number already exists
     */
    public boolean isPhoneExists(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT 1 FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE phoneNumber = ? LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{phone});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Login user - verify credentials
     * @return userId if successful, -1 if failed
     */
    public long loginUser(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT userId FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE email = ? AND password = ? AND isActive = 1";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});

        long userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0);
        }
        cursor.close();
        return userId;
    }

    /**
     * Get user by ID
     */
    public Cursor getUserById(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE userId = ?";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }

    /**
     * Update user profile
     */
    public boolean updateUser(long userId, String name, String phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("fullName", name);
        values.put("phoneNumber", phone);

        int rows = db.update(MessKhataDatabase.TABLE_USERS,
                values,
                "userId = ?",
                new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    /**
     * Delete user (soft delete - set isActive to 0)
     */
    public boolean deleteUser(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("isActive", 0);

        int rows = db.update(MessKhataDatabase.TABLE_USERS,
                values,
                "userId = ?",
                new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    /**
     * Get user's mess ID
     * @return messId if user has mess, -1 if no mess
     */
    public int getUserMessId(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT messId FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE userId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        int messId = -1;
        if (cursor.moveToFirst()) {
            if (!cursor.isNull(0)) {
                messId = cursor.getInt(0);
            }
        }
        cursor.close();
        return messId;
    }

    /**
     * Get user's complete info including mess details
     * Note: invitationCode is calculated from messId (messId + 999)
     */
    public Cursor getUserWithMess(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT u.*, m.messName " +
                "FROM " + MessKhataDatabase.TABLE_USERS + " u " +
                "LEFT JOIN " + MessKhataDatabase.TABLE_MESS + " m " +
                "ON u.messId = m.messId " +
                "WHERE u.userId = ?";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }
}