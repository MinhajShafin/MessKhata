package com.messkhata.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.utils.Constants;
import com.messkhata.utils.PreferenceManager;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            handleDataMessage(type, remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save the token for later use
        PreferenceManager.getInstance().setFcmToken(token);

        // If user is logged in, update token on server
        String userId = PreferenceManager.getInstance().getUserId();
        if (userId != null) {
            // Update token in Firebase
            updateTokenOnServer(userId, token);
        }
    }

    private void showNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, Constants.NOTIFICATION_CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void handleDataMessage(String type, java.util.Map<String, String> data) {
        if (type == null) return;

        switch (type) {
            case "EXPENSE_ADDED":
            case "MEAL_UPDATED":
            case "REPORT_GENERATED":
                // Trigger sync to get latest data
                triggerSync();
                break;
            case "MEMBER_ADDED":
            case "MEMBER_REMOVED":
                // Refresh member list
                triggerSync();
                break;
        }
    }

    private void triggerSync() {
        SyncWorker.enqueueImmediateSync(this);
    }

    private void updateTokenOnServer(String userId, String token) {
        // This would update the FCM token in Firebase for push notifications
        // Implementation depends on your Firebase structure
    }
}
