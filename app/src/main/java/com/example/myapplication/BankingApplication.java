package com.example.myapplication;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.FirebaseApp;
import com.example.myapplication.utils.Constants;

/**
 * Application class for initializing Firebase and other services
 */
public class BankingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Create notification channels
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Transaction notification channel
            NotificationChannel transactionChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_TRANSACTION,
                    "Thông báo giao dịch",
                    NotificationManager.IMPORTANCE_HIGH
            );
            transactionChannel.setDescription("Thông báo về các giao dịch ngân hàng");
            notificationManager.createNotificationChannel(transactionChannel);

            // Promotion notification channel
            NotificationChannel promotionChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_PROMOTION,
                    "Khuyến mãi",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            promotionChannel.setDescription("Thông báo về các chương trình khuyến mãi");
            notificationManager.createNotificationChannel(promotionChannel);

            // Security notification channel
            NotificationChannel securityChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_SECURITY,
                    "Bảo mật",
                    NotificationManager.IMPORTANCE_HIGH
            );
            securityChannel.setDescription("Thông báo về bảo mật tài khoản");
            notificationManager.createNotificationChannel(securityChannel);
        }
    }
}
