package com.gigya.android.sdk.tfa.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.ui.GigyaPushTfaActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Main FCM messaging service.
 * Extend this service if your application already uses the FirebaseMessagingService.
 */
public class GigyaMessagingService extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        // Safe to call here. Once notification channel is created it won't be recreated again.
        createTFANotificationChannel();
    }

    final private static String LOG_TAG = "GigyaMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        GigyaLogger.debug(LOG_TAG, "RemoteMessage: " + remoteMessage.toString());

        if (remoteMessage.getData().size() > 0) {
            // Data available and should be treated accordingly.
        }

    }

    @Override
    public void onNewToken(String fcmToken) {
        GigyaLogger.debug(LOG_TAG, "onNewToken: " + fcmToken);
    }

    private static final String CHANNEL_ID = "tfa_channel";

    private void createTFANotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.tfa_channel_name);
            String description = getString(R.string.tfa_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private PendingIntent getContentActivityPendingIntent() {
        Intent notifyIntent = new Intent(this, GigyaPushTfaActivity.class);
        // Set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Create the PendingIntent
        return PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * Attempt to cancel a displayed notification given a unique identification.
     *
     * @param notificationId Currently displayed notification id.
     */
    private void cancel(int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(notificationId);
    }
}
