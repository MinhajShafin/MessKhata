package com.messkhata.utils;

/**
 * Constants used throughout the application.
 */
public class Constants {

    // Shared Preferences
    public static final String PREFS_NAME = "messkhata_prefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_MESS_ID = "mess_id";
    public static final String PREF_USER_ROLE = "user_role";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_LAST_SYNC = "last_sync";

    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_MESSES = "messes";
    public static final String COLLECTION_MEALS = "meals";
    public static final String COLLECTION_EXPENSES = "expenses";
    public static final String COLLECTION_REPORTS = "monthly_reports";
    public static final String COLLECTION_BALANCES = "member_balances";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    // User Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_MEMBER = "MEMBER";

    // Meal Types
    public static final String MEAL_BREAKFAST = "BREAKFAST";
    public static final String MEAL_LUNCH = "LUNCH";
    public static final String MEAL_DINNER = "DINNER";

    // Expense Categories
    public static final String EXPENSE_GROCERY = "GROCERY";
    public static final String EXPENSE_UTILITY = "UTILITY";
    public static final String EXPENSE_GAS = "GAS";
    public static final String EXPENSE_RENT = "RENT";
    public static final String EXPENSE_MAINTENANCE = "MAINTENANCE";
    public static final String EXPENSE_OTHER = "OTHER";

    // Sync Actions
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    // Notification Types
    public static final String NOTIF_PAYMENT_REMINDER = "PAYMENT_REMINDER";
    public static final String NOTIF_MEAL_ENTRY_REMINDER = "MEAL_ENTRY_REMINDER";
    public static final String NOTIF_NEW_EXPENSE = "NEW_EXPENSE";
    public static final String NOTIF_REPORT_READY = "REPORT_READY";
    public static final String NOTIF_GENERAL = "GENERAL";

    // WorkManager Tags
    public static final String WORK_SYNC = "sync_work";
    public static final String WORK_REMINDER = "reminder_work";

    // Notification Channels
    public static final String NOTIFICATION_CHANNEL_ID = "messkhata_notifications";
    public static final String NOTIFICATION_CHANNEL_NAME = "MessKhata Notifications";
    public static final String NOTIFICATION_CHANNEL_GENERAL = "messkhata_general";
    public static final String NOTIFICATION_CHANNEL_REMINDERS = "messkhata_reminders";
    public static final String NOTIFICATION_CHANNEL_SYNC = "messkhata_sync";

    // Request Codes
    public static final int REQUEST_CODE_NOTIFICATION = 1001;

    // Bundle Keys
    public static final String KEY_DATE = "date";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_MESS_ID = "mess_id";
    public static final String KEY_EXPENSE_ID = "expense_id";
    public static final String KEY_REPORT_ID = "report_id";
    public static final String KEY_YEAR = "year";
    public static final String KEY_MONTH = "month";
}
