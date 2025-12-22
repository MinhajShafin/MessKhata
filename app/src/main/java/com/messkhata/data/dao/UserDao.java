package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
     * 
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
        Cursor cursor = db.rawQuery(query, new String[] { email });

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Get user by email (returns Cursor for sync operations)
     */
    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE email = ? AND isActive = 1 LIMIT 1";
        return db.rawQuery(query, new String[] { email });
    }

    /**
     * Check if phone number already exists
     */
    public boolean isPhoneExists(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT 1 FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE phoneNumber = ? LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[] { phone });

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Login user - verify credentials
     * 
     * @return userId if successful, -1 if failed
     */
    public long loginUser(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT userId FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE email = ? AND password = ? AND isActive = 1";
        Cursor cursor = db.rawQuery(query, new String[] { email, password });

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
        return db.rawQuery(query, new String[] { String.valueOf(userId) });
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
                new String[] { String.valueOf(userId) });
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
                new String[] { String.valueOf(userId) });
        return rows > 0;
    }

    /**
     * Get user's mess ID
     * 
     * @return messId if user has mess, -1 if no mess
     */
    public int getUserMessId(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT messId FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE userId = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(userId) });

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
        return db.rawQuery(query, new String[] { String.valueOf(userId) });
    }

    /**
     * Get user by ID as User object
     * 
     * @return User object or null if not found
     */
    public User getUserByIdAsObject(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE userId = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(userId) });

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow("userId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")),
                    cursor.isNull(cursor.getColumnIndexOrThrow("messId")) ? -1
                            : cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("joinedDate")));
        }
        cursor.close();
        return user;
    }

    /**
     * Get all members of a mess
     * 
     * @return List of User objects
     */
    public List<User> getMembersByMessId(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> members = new ArrayList<>();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE messId = ? AND isActive = 1 ORDER BY role DESC, fullName ASC";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(messId) });

        while (cursor.moveToNext()) {
            User user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow("userId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("joinedDate")));
            members.add(user);
        }
        cursor.close();
        return members;
    }

    /**
     * Get all users in a mess as Cursor (for sync operations)
     * 
     * @return Cursor with user data
     */
    public Cursor getUsersByMessId(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE messId = ? AND isActive = 1";
        return db.rawQuery(query, new String[] { String.valueOf(messId) });
    }

    /**
     * Update user's mess ID
     * 
     * @return true if successful
     */
    public boolean updateUserMessId(long userId, int messId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("messId", messId);

        int rows = db.update(MessKhataDatabase.TABLE_USERS,
                values,
                "userId = ?",
                new String[] { String.valueOf(userId) });
        return rows > 0;
    }

    /**
     * Update user's role (admin only)
     * 
     * @return true if successful
     */
    public boolean updateUserRole(long userId, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("role", role);

        int rows = db.update(MessKhataDatabase.TABLE_USERS,
                values,
                "userId = ?",
                new String[] { String.valueOf(userId) });
        return rows > 0;
    }

    /**
     * Get user's role
     * 
     * @return Role string ("admin" or "member") or null if user not found
     */
    public String getUserRole(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT role FROM " + MessKhataDatabase.TABLE_USERS +
                " WHERE userId = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(userId) });

        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }
        cursor.close();
        return role;
    }

    /**
     * Check if user is admin
     * 
     * @return true if user is admin
     */
    public boolean isUserAdmin(long userId) {
        String role = getUserRole(userId);
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Get active members on a specific date
     * A member is considered active if they have any meal entries on that date
     * @param messId The mess ID
     * @param date The date timestamp (in seconds)
     * @return List of User IDs who had meals on that date
     */
    public List<Integer> getActiveMembersByDate(int messId, long date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Integer> activeMembers = new ArrayList<>();

        String query = "SELECT DISTINCT userId FROM " + MessKhataDatabase.TABLE_MEALS +
                " WHERE messId = ? AND mealDate = ? AND (breakfast > 0 OR lunch > 0 OR dinner > 0)";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(date)
        });

        while (cursor.moveToNext()) {
            activeMembers.add(cursor.getInt(0));
        }
        cursor.close();
        return activeMembers;
    }

    /**
     * Get count of active members for a specific month
     * Returns count of all active members in the mess who joined before the month ended
     * @param messId The mess ID
     * @param month The month (1-12)
     * @param year The year
     * @return Count of active members
     */
    public int getActiveMemberCount(int messId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Calculate start and end timestamps for the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        long startDate = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.MONTH, 1);
        long endDate = calendar.getTimeInMillis() / 1000;

        // Count all active members who joined before the end of the month
        String query = "SELECT COUNT(*) as count FROM " + 
                MessKhataDatabase.TABLE_USERS +
                " WHERE messId = ? AND isActive = 1 AND joinedDate < ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{
            String.valueOf(messId),
            String.valueOf(endDate)
        });

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}