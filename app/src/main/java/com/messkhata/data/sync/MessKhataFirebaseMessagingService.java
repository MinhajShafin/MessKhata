package com.messkhata.data.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.utils.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * Receives notifications when other mess members add expenses, meals, etc.
 */
public class MessKhataFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    // Notification channels
    public static final String CHANNEL_EXPENSE = "expense_notifications";
    public static final String CHANNEL_MEAL = "meal_notifications";
    public static final String CHANNEL_MEMBER = "member_notifications";
    public static final String CHANNEL_GENERAL = "general_notifications";

    // Notification types
    public static final String TYPE_EXPENSE_ADDED = "expense_added";
    public static final String TYPE_MEAL_UPDATED = "meal_updated";
    public static final String TYPE_MEMBER_JOINED = "member_joined";
    public static final String TYPE_MEMBER_LEFT = "member_left";
    public static final String TYPE_MESS_UPDATED = "mess_updated";

    // Broadcast action for local notification
    public static final String ACTION_FCM_RECEIVED = "com.messkhata.FCM_RECEIVED";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    /**
     * Called when a new FCM token is generated
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);

        // Save token locally
        PreferenceManager.getInstance(this).setFcmToken(token);

        // Send token to server
        sendTokenToServer(token);
    }

    /**
     * Called when a message is received
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData());
        }
    }

    /**
     * Handle data message (silent notification)
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String messId = data.get("messId");
        String senderEmail = data.get("senderEmail");

        // Don't show notification for own actions
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null
                && currentUser.getEmail().equals(senderEmail)) {
            Log.d(TAG, "Ignoring notification for own action");
            return;
        }

        // Check if this notification is for user's mess
        String userMessId = PreferenceManager.getInstance(this).getFirebaseMessId();
        if (messId != null && !messId.equals(userMessId)) {
            Log.d(TAG, "Ignoring notification for different mess");
            return;
        }

        // Broadcast to refresh UI
        Intent broadcastIntent = new Intent(ACTION_FCM_RECEIVED);
        broadcastIntent.putExtra("type", type);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        // Also trigger data refresh broadcast
        Intent refreshIntent = new Intent(RealtimeSyncManager.ACTION_DATA_UPDATED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(refreshIntent);

        // Show notification based on type
        if (type != null) {
            switch (type) {
                case TYPE_EXPENSE_ADDED:
                    showExpenseNotification(data);
                    break;
                case TYPE_MEAL_UPDATED:
                    showMealNotification(data);
                    break;
                case TYPE_MEMBER_JOINED:
                    showMemberJoinedNotification(data);
                    break;
                case TYPE_MEMBER_LEFT:
                    showMemberLeftNotification(data);
                    break;
                default:
                    Log.d(TAG, "Unknown notification type: " + type);
            }
        }
    }

    /**
     * Handle notification message (display notification)
     */
    private void handleNotificationMessage(String title, String body, Map<String, String> data) {
        // Check if this notification is for user's mess
        String messId = data.get("messId");
        String userMessId = PreferenceManager.getInstance(this).getFirebaseMessId();
        if (messId != null && !messId.equals(userMessId)) {
            return;
        }

        showNotification(title, body, CHANNEL_GENERAL, 0);
    }

    /**
     * Show expense notification
     */
    private void showExpenseNotification(Map<String, String> data) {
        String memberName = data.get("memberName");
        String amount = data.get("amount");
        String category = data.get("category");

        String title = "New Expense Added";
        String body = memberName + " added ৳" + amount + " for " + category;

        showNotification(title, body, CHANNEL_EXPENSE, 1);
    }

    /**
     * Show meal notification
     */
    private void showMealNotification(Map<String, String> data) {
        String memberName = data.get("memberName");
        String mealCount = data.get("mealCount");

        String title = "Meal Updated";
        String body = memberName + " updated their meal count to " + mealCount;

        showNotification(title, body, CHANNEL_MEAL, 2);
    }

    /**
     * Show member joined notification
     */
    private void showMemberJoinedNotification(Map<String, String> data) {
        String memberName = data.get("memberName");

        String title = "New Member";
        String body = memberName + " joined the mess";

        showNotification(title, body, CHANNEL_MEMBER, 3);
    }

    /**
     * Show member left notification
     */
    private void showMemberLeftNotification(Map<String, String> data) {
        String memberName = data.get("memberName");

        String title = "Member Left";
        String body = memberName + " left the mess";

        showNotification(title, body, CHANNEL_MEMBER, 4);
    }

    /**
     * Display notification
     */
    private void showNotification(String title, String body, String channelId, int notificationId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    /**
     * Create notification channels for Android O+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);

            if (notificationManager == null)
                return;

            // Expense channel
            NotificationChannel expenseChannel = new NotificationChannel(
                    CHANNEL_EXPENSE,
                    "Expense Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            expenseChannel.setDescription("Notifications for new expenses");
            notificationManager.createNotificationChannel(expenseChannel);

            // Meal channel
            NotificationChannel mealChannel = new NotificationChannel(
                    CHANNEL_MEAL,
                    "Meal Notifications",
                    NotificationManager.IMPORTANCE_LOW);
            mealChannel.setDescription("Notifications for meal updates");
            notificationManager.createNotificationChannel(mealChannel);

            // Member channel
            NotificationChannel memberChannel = new NotificationChannel(
                    CHANNEL_MEMBER,
                    "Member Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            memberChannel.setDescription("Notifications for member changes");
            notificationManager.createNotificationChannel(memberChannel);

            // General channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("General notifications");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }

    /**
     * Send FCM token to Firestore for this user
     */
    private void sendTokenToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping token upload");
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            return;
        }

        // Save token to user's document in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        tokenData.put("lastUpdated", System.currentTimeMillis());

        db.collection("fcm_tokens")
                .document(email)
                .set(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved to server"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
    }

    /**
     * Get current FCM token and send to server
     */
    public static void refreshToken(Context context) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error getting FCM token", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save locally
                    PreferenceManager.getInstance(context).setFcmToken(token);

                    // Send to server
                    new MessKhataFirebaseMessagingService().sendTokenToServer(token);
                });
    }
}
