package com.messkhata.data.sync;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.messkhata.utils.PreferenceManager;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for sending push notifications to other mess members.
 *
 * Note: For production, you should use Firebase Cloud Functions to send
 * notifications securely. This is a simplified approach that stores
 * notification data in Firestore which can trigger a Cloud Function.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static NotificationHelper instance;

    private final Context context;
    private final FirebaseFirestore db;
    private final ExecutorService executor;

    // Notification types
    public static final String TYPE_EXPENSE_ADDED = "expense_added";
    public static final String TYPE_MEAL_UPDATED = "meal_updated";
    public static final String TYPE_MEMBER_JOINED = "member_joined";
    public static final String TYPE_MEMBER_LEFT = "member_left";

    private NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized NotificationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
        return instance;
    }

    /**
     * Notify mess members about a new expense
     */
    public void notifyExpenseAdded(String firebaseMessId, String category, double amount) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        String memberName = PreferenceManager.getInstance(context).getUserName();
        if (memberName == null)
            memberName = "A member";

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_EXPENSE_ADDED);
        data.put("messId", firebaseMessId);
        data.put("memberName", memberName);
        data.put("amount", String.format("%.2f", amount));
        data.put("category", category);
        data.put("senderEmail", currentUser.getEmail());

        String title = "New Expense Added";
        String body = memberName + " added ৳" + String.format("%.2f", amount) + " for " + category;

        sendNotificationToMess(firebaseMessId, title, body, data);
    }

    /**
     * Notify mess members about meal update
     */
    public void notifyMealUpdated(String firebaseMessId, int totalMeals) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        String memberName = PreferenceManager.getInstance(context).getUserName();
        if (memberName == null)
            memberName = "A member";

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_MEAL_UPDATED);
        data.put("messId", firebaseMessId);
        data.put("memberName", memberName);
        data.put("mealCount", String.valueOf(totalMeals));
        data.put("senderEmail", currentUser.getEmail());

        String title = "Meal Updated";
        String body = memberName + " updated their meal count to " + totalMeals;

        sendNotificationToMess(firebaseMessId, title, body, data);
    }

    /**
     * Notify mess members about new member joining
     */
    public void notifyMemberJoined(String firebaseMessId, String newMemberName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_MEMBER_JOINED);
        data.put("messId", firebaseMessId);
        data.put("memberName", newMemberName);
        data.put("senderEmail", currentUser.getEmail());

        String title = "New Member";
        String body = newMemberName + " joined the mess";

        sendNotificationToMess(firebaseMessId, title, body, data);
    }

    /**
     * Notify mess members about member leaving
     */
    public void notifyMemberLeft(String firebaseMessId, String memberName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_MEMBER_LEFT);
        data.put("messId", firebaseMessId);
        data.put("memberName", memberName);
        data.put("senderEmail", currentUser.getEmail());

        String title = "Member Left";
        String body = memberName + " left the mess";

        sendNotificationToMess(firebaseMessId, title, body, data);
    }

    /**
     * Send notification to all members of a mess.
     * This stores a notification document in Firestore which can be:
     * 1. Processed by a Cloud Function to send actual FCM notifications
     * 2. Picked up by clients when they come online
     */
    private void sendNotificationToMess(String firebaseMessId, String title, String body,
            Map<String, String> data) {
        executor.execute(() -> {
            try {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.d(TAG, "No current user, skipping notification");
                    return;
                }

                // Create notification document
                Map<String, Object> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                notification.put("data", data);
                notification.put("messId", firebaseMessId);
                notification.put("senderEmail", currentUser.getEmail());
                notification.put("timestamp", System.currentTimeMillis());
                notification.put("processed", false);

                // Save to Firestore - a Cloud Function can listen to this collection
                // and send actual FCM notifications
                db.collection("notifications")
                        .add(notification)
                        .addOnSuccessListener(docRef -> {
                            Log.d(TAG, "Notification queued: " + docRef.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error queuing notification", e);
                        });

                // Also send directly to FCM tokens of mess members
                sendDirectNotifications(firebaseMessId, title, body, data, currentUser.getEmail());

            } catch (Exception e) {
                Log.e(TAG, "Error sending notification", e);
            }
        });
    }

    /**
     * Send direct FCM notifications to mess members.
     * Gets FCM tokens from Firestore and sends notifications.
     *
     * Note: For production, this should be done via Cloud Functions for security.
     * The FCM server key should never be in client code.
     */
    private void sendDirectNotifications(String firebaseMessId, String title, String body,
            Map<String, String> data, String senderEmail) {
        try {
            // Get all users in this mess
            QuerySnapshot usersSnapshot = Tasks.await(
                    db.collection("messes")
                            .document(firebaseMessId)
                            .collection("users")
                            .get());

            for (DocumentSnapshot userDoc : usersSnapshot.getDocuments()) {
                String userEmail = userDoc.getString("email");

                // Skip sender
                if (userEmail != null && userEmail.equals(senderEmail)) {
                    continue;
                }

                // Get FCM token for this user
                DocumentSnapshot tokenDoc = Tasks.await(
                        db.collection("fcm_tokens").document(userEmail).get());

                if (tokenDoc.exists()) {
                    String fcmToken = tokenDoc.getString("fcmToken");
                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        // Store pending notification for this user
                        // (actual sending should be done via Cloud Functions)
                        Map<String, Object> pendingNotification = new HashMap<>();
                        pendingNotification.put("token", fcmToken);
                        pendingNotification.put("title", title);
                        pendingNotification.put("body", body);
                        pendingNotification.put("data", data);
                        pendingNotification.put("timestamp", System.currentTimeMillis());

                        db.collection("pending_notifications")
                                .add(pendingNotification)
                                .addOnSuccessListener(
                                        docRef -> Log.d(TAG, "Pending notification created for: " + userEmail))
                                .addOnFailureListener(e -> Log.e(TAG, "Error creating pending notification", e));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending direct notifications", e);
        }
    }
}
