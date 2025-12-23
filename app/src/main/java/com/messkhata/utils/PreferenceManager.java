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

    /**
     * Clear only mess-related session data (used when leaving a mess)
     * Keeps user logged in but removes mess association
     */
    public void clearMessSession() {
        preferences.edit()
                .remove(Constants.PREF_MESS_ID)
                .remove(Constants.PREF_USER_ROLE)
                .apply();
    }

    public String getUserId() {
        return preferences.getString(Constants.PREF_USER_ID, null);
    }

    public String getMessId() {
        return preferences.getString(Constants.PREF_MESS_ID, null);
    }

    public String getUserRole() {
        return preferences.getString(Constants.PREF_USER_ROLE, null);
    }

    public String getUserName() {
        return preferences.getString(Constants.PREF_USER_NAME, null);
    }

    public String getUserEmail() {
        return preferences.getString(Constants.PREF_USER_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    // FCM Token Management
    public void setFcmToken(String token) {
        preferences.edit()
                .putString("fcm_token", token)
                .apply();
    }

    public String getFcmToken() {
        return preferences.getString("fcm_token", null);
    }

    // Firebase Mess ID for notifications
    public void setFirebaseMessId(String firebaseMessId) {
        preferences.edit()
                .putString("firebase_mess_id", firebaseMessId)
                .apply();
    }

    public String getFirebaseMessId() {
        return preferences.getString("firebase_mess_id", null);
    }

}
