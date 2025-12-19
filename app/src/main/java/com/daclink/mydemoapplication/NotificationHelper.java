package com.daclink.mydemoapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.daclink.mydemoapplication.R;
/*
 * Author: France Zhang
 * Created on: 12/17/2025
        * Description: Notification class
 */

public class NotificationHelper {

    public static final String CHANNEL_ID = "SMARTCARDS_CHANNEL";

    private NotificationHelper() {
        // Utility class – no instances
    }

    /**
     * Ensure the notification channel exists (required for Android 8+)
     */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartCards Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for SmartCards admin actions");

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show a system notification
     */
    public static void show(Context context, int notificationId,
                            String title, String message) {

        ensureChannel(context);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManagerCompat
                .from(context)
                .notify(notificationId, builder.build());
    }
}
