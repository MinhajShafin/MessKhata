package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Mess;

public class MessDao {

    private MessKhataDatabase dbHelper;

    public MessDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
    }

    /**
     * Convert messId to display invitation code
     * messId 1 → code "1000"
     * messId 2 → code "1001"
     */
    public String getInvitationCode(int messId) {
        return String.valueOf(messId + 999);
    }

    /**
     * Convert invitation code to messId
     * code "1000" → messId 1
     * code "1001" → messId 2
     */
    public int getMessIdFromCode(String invitationCode) {
        try {
            int code = Integer.parseInt(invitationCode);
            return code - 999;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Create new mess
     * 
     * @return messId if successful, -1 if failed
     */
    public long createMess(String messName, double groceryBudget,
            double cookingCharge, long creatorUserId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Insert mess (messId auto-generated)
            ContentValues messValues = new ContentValues();
            messValues.put("messName", messName);
            messValues.put("groceryBudgetPerMeal", groceryBudget);
            messValues.put("cookingChargePerMeal", cookingCharge);
            messValues.put("createdDate", System.currentTimeMillis() / 1000);

            long messId = db.insert(MessKhataDatabase.TABLE_MESS, null, messValues);

            if (messId != -1) {
                // Update creator's messId and role to admin
                ContentValues userValues = new ContentValues();
                userValues.put("messId", messId);
                userValues.put("role", "admin");

                db.update(MessKhataDatabase.TABLE_USERS,
                        userValues,
                        "userId = ?",
                        new String[] { String.valueOf(creatorUserId) });
            }

            return messId;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Join existing mess using invitation code
     * 
     * @return true if successful, false if failed
     */
    public boolean joinMess(String invitationCode, long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Convert code to messId
            int messId = getMessIdFromCode(invitationCode);

            if (messId <= 0) {
                return false; // Invalid code format
            }

            // Check if mess exists
            String query = "SELECT messId FROM " + MessKhataDatabase.TABLE_MESS +
                    " WHERE messId = ?";
            Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(messId) });

            if (cursor.moveToFirst()) {
                cursor.close();

                // Update user's messId and role to member
                ContentValues values = new ContentValues();
                values.put("messId", messId);
                values.put("role", "member");

                int rows = db.update(MessKhataDatabase.TABLE_USERS,
                        values,
                        "userId = ?",
                        new String[] { String.valueOf(userId) });
                return rows > 0;
            }

            cursor.close();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get mess by invitation code
     */
    public Cursor getMessByCode(String invitationCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int messId = getMessIdFromCode(invitationCode);

        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MESS +
                " WHERE messId = ?";
        return db.rawQuery(query, new String[] { String.valueOf(messId) });
    }

    /**
     * Get mess by ID
     */
    public Cursor getMessById(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MESS +
                " WHERE messId = ?";
        return db.rawQuery(query, new String[] { String.valueOf(messId) });
    }

    /**
     * Update mess rates
     */
    public boolean updateMessRates(int messId, double groceryBudget, double cookingCharge) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("groceryBudgetPerMeal", groceryBudget);
        values.put("cookingChargePerMeal", cookingCharge);

        int rows = db.update(MessKhataDatabase.TABLE_MESS,
                values,
                "messId = ?",
                new String[] { String.valueOf(messId) });
        return rows > 0;
    }

    /**
     * Get mess by ID as Mess object
     * 
     * @return Mess object or null if not found
     */
    public Mess getMessByIdAsObject(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MESS +
                " WHERE messId = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(messId) });

        Mess mess = null;
        if (cursor.moveToFirst()) {
            mess = new Mess(
                    cursor.getInt(cursor.getColumnIndexOrThrow("messId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("messName")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("groceryBudgetPerMeal")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("cookingChargePerMeal")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("createdDate")));
        }
        cursor.close();
        return mess;
    }

    /**
     * Get mess name by ID
     * 
     * @return Mess name or null if not found
     */
    public String getMessNameById(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT messName FROM " + MessKhataDatabase.TABLE_MESS +
                " WHERE messId = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(messId) });

        String messName = null;
        if (cursor.moveToFirst()) {
            messName = cursor.getString(0);
        }
        cursor.close();
        return messName;
    }

    /**
     * Update mess name
     * 
     * @return true if successful
     */
    public boolean updateMessName(int messId, String messName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("messName", messName);

        int rows = db.update(MessKhataDatabase.TABLE_MESS,
                values,
                "messId = ?",
                new String[] { String.valueOf(messId) });
        return rows > 0;
    }

    /**
     * Save Firebase mess ID and invitation code mapping
     * Uses SharedPreferences for simplicity (could also use a separate table)
     */
    public void saveFirebaseMessId(int localMessId, String firebaseMessId, String invitationCode) {
        android.content.SharedPreferences prefs = dbHelper.getWritableDatabase()
                .getPath() != null ? android.preference.PreferenceManager.getDefaultSharedPreferences(
                        com.messkhata.MessKhataApplication.getInstance()) : null;

        if (prefs != null) {
            prefs.edit()
                    .putString("firebase_mess_id_" + localMessId, firebaseMessId)
                    .putString("invitation_code_" + localMessId, invitationCode)
                    .putInt("local_mess_id_" + firebaseMessId, localMessId)
                    .apply();
        }
    }

    /**
     * Get Firebase mess ID for a local mess ID
     */
    public String getFirebaseMessId(int localMessId) {
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(
                com.messkhata.MessKhataApplication.getInstance());
        return prefs.getString("firebase_mess_id_" + localMessId, null);
    }

    /**
     * Get local mess ID by Firebase ID
     */
    public int getMessIdByFirebaseId(String firebaseMessId) {
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(
                com.messkhata.MessKhataApplication.getInstance());
        return prefs.getInt("local_mess_id_" + firebaseMessId, -1);
    }

    /**
     * Get saved invitation code for a local mess
     */
    public String getSavedInvitationCode(int localMessId) {
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(
                com.messkhata.MessKhataApplication.getInstance());
        return prefs.getString("invitation_code_" + localMessId, getInvitationCode(localMessId));
    }

    /**
     * Create mess with specific details (for joining from Firebase)
     * Does NOT set creator as admin
     */
    public long createMessWithDetails(String messName, double groceryBudget,
            double cookingCharge, long createdDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues messValues = new ContentValues();
            messValues.put("messName", messName);
            messValues.put("groceryBudgetPerMeal", groceryBudget);
            messValues.put("cookingChargePerMeal", cookingCharge);
            messValues.put("createdDate", createdDate);

            return db.insert(MessKhataDatabase.TABLE_MESS, null, messValues);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
