package com.messkhata.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for managing SharedPreferences.
 */
public class PreferenceManager {

    private static PreferenceManager instance;
    private final SharedPreferences preferences;

    private PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized PreferenceManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PreferenceManager not initialized. Call init(Context) first.");
        }
        return instance;
    }

    // User Session Management
    public void saveUserSession(String userId, String messId, String role,
                                 String name, String email) {
        preferences.edit()
                .putString(Constants.PREF_USER_ID, userId)
                .putString(Constants.PREF_MESS_ID, messId)
                .putString(Constants.PREF_USER_ROLE, role)
                .putString(Constants.PREF_USER_NAME, name)
                .putString(Constants.PREF_USER_EMAIL, email)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .apply();
    }

    public void clearSession() {
        preferences.edit()
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_MESS_ID)
                .remove(Constants.PREF_USER_ROLE)
                .remove(Constants.PREF_USER_NAME)
                .remove(Constants.PREF_USER_EMAIL)
                .putBoolean(Constants.PREF_IS_LOGGED_IN, false)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return preferences.getString(Constants.PREF_USER_ID, null);
    }

    public String getMessId() {
        return preferences.getString(Constants.PREF_MESS_ID, null);
    }

    public void setMessId(String messId) {
        preferences.edit().putString(Constants.PREF_MESS_ID, messId).apply();
    }

    public String getUserRole() {
        return preferences.getString(Constants.PREF_USER_ROLE, Constants.ROLE_MEMBER);
    }

    public void setUserRole(String role) {
        preferences.edit().putString(Constants.PREF_USER_ROLE, role).apply();
    }

    public String getUserName() {
        return preferences.getString(Constants.PREF_USER_NAME, "");
    }

    public String getUserEmail() {
        return preferences.getString(Constants.PREF_USER_EMAIL, "");
    }

    // Role Check Helpers
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getUserRole());
    }

    public boolean isManager() {
        return Constants.ROLE_MANAGER.equals(getUserRole());
    }

    public boolean isAdminOrManager() {
        String role = getUserRole();
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    // Sync Management
    public void setLastSyncTime(long timestamp) {
        preferences.edit().putLong(Constants.PREF_LAST_SYNC, timestamp).apply();
    }

    public long getLastSyncTime() {
        return preferences.getLong(Constants.PREF_LAST_SYNC, 0);
    }

    // Generic methods
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    // FCM Token
    private static final String PREF_FCM_TOKEN = "fcm_token";

    public void setFcmToken(String token) {
        preferences.edit().putString(PREF_FCM_TOKEN, token).apply();
    }

    public String getFcmToken() {
        return preferences.getString(PREF_FCM_TOKEN, null);
    }
}
