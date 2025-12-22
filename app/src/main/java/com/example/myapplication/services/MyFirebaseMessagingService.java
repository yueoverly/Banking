package com.example.myapplication.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.myapplication.R;
import com.example.myapplication.activities.MainActivity;
import com.example.myapplication.utils.SessionManager;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "banking_notifications";
    private static final String CHANNEL_NAME = "Banking Notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save token to server for push notifications
        saveTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage.getData());
        }
    }

    private void saveTokenToServer(String token) {
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        
        if (userId != null && !userId.isEmpty()) {
            // Save FCM token to Firestore for the current user
            // This can be used to send targeted push notifications
        }
    }

    private void handleDataMessage(java.util.Map<String, String> data) {
        String type = data.get("type");
        String message = data.get("message");
        
        if (type != null) {
            switch (type) {
                case "transaction":
                    showNotification("Giao dịch mới", message);
                    break;
                case "otp":
                    showNotification("Mã xác thực", message);
                    break;
                case "promotion":
                    showNotification("Khuyến mãi", message);
                    break;
                default:
                    showNotification("Thông báo", message);
                    break;
            }
        }
    }

    private void showNotification(String title, String message) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Banking app notifications");
            channel.enableVibration(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
