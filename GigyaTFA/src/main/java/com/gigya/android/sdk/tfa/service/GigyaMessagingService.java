package com.gigya.android.sdk.tfa.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.ui.GigyaPushTfaActivity;
import com.gigya.android.sdk.tfa.worker.TokenUpdateWorker;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Main FCM messaging service.
 * Extend this service if your application already uses the FirebaseMessagingService.
 */
public abstract class GigyaMessagingService extends FirebaseMessagingService {

    final private static String LOG_TAG = "GigyaMessagingService";

    protected abstract int getNotificationIcon();

    @Override
    public void onCreate() {
        // Safe to call here. Once notification channel is created it won't be recreated again.
        createTFANotificationChannel();
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

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        GigyaLogger.debug(LOG_TAG, "RemoteMessage: " + remoteMessage.toString());

        if (remoteMessage.getData().size() > 0) {
            // Check data purpose and continue flow accordingly.
            if (isCancelMessage()) {
                // TODO: 2019-06-12 Fetch notification id from the data structure.
                final int id = 0;
                cancel(id);
            } else {
                notifyWith(remoteMessage.getData());
            }
        }
    }

    @Override
    public void onNewToken(String fcmToken) {
        GigyaLogger.debug(LOG_TAG, "onNewToken: " + fcmToken);
        // TODO: 2019-06-12 Check if token should be updated. If so send update task to server.
        sendTokenToServer(fcmToken);
    }

    private boolean isCancelMessage() {
        return false;
    }

    private void notifyWith(Map<String, String> data) {
        // Fetch the data.
        final int androidNotificationId = 0;
        final String title = "My notification";
        final String content = "Hello World!";

        Intent intent = new Intent(this, GigyaPushTfaActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        // Deny action.
        Intent denyIntent = new Intent(this, GigyaTFAActionReceiver.class);
        denyIntent.setAction(getString(R.string.tfa_action_deny));
        denyIntent.putExtra("notificationId", androidNotificationId);
        PendingIntent denyPendingIntent =
                PendingIntent.getBroadcast(this, 0, denyIntent, 0);

        // Approve action.
        Intent approveIntent = new Intent(this, GigyaTFAActionReceiver.class);
        approveIntent.setAction(getString(R.string.tfa_action_approve));
        approveIntent.putExtra("notificationId", androidNotificationId);
        PendingIntent approvePendingIntent =
                PendingIntent.getBroadcast(this, 0, approveIntent, 0);

        // Build notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.bg_gigya_custom)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(getNotificationIcon(), getString(R.string.deny),
                        denyPendingIntent)
                .addAction(getNotificationIcon(), getString(R.string.approve),
                        approvePendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(androidNotificationId, builder.build());
    }

    private PendingIntent getContentBroadcasePendingIntent() {
        Intent notifyIntent = new Intent(this, null);
        return PendingIntent.getBroadcast(this, GigyaDefinitions.Codes.PUST_TFA_CONTENT_ACTION_REQUEST_CODE,
                notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    // TODO: 2019-06-12 Not entirely sure we need an activity initiator.
    private PendingIntent getContentActivityPendingIntent() {
        Intent notifyIntent = new Intent(this, GigyaPushTfaActivity.class);
        // Set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Create the PendingIntent
        return PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT
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

    /**
     * Start a worker task to update the Firebase token.
     *
     * @param fcmToken Received Token.
     */
    private void sendTokenToServer(String fcmToken) {
        OneTimeWorkRequest.Builder updateWorkRequestBuilder = new OneTimeWorkRequest.Builder(TokenUpdateWorker.class);
        Data inputData = new Data.Builder().putString("token", fcmToken).build();
        updateWorkRequestBuilder.setInputData(inputData);
        WorkManager.getInstance().enqueue(updateWorkRequestBuilder.build());
    }
}
