package com.gigya.android.sdk.tfa;

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
import com.gigya.android.sdk.tfa.ui.GigyaPushTfaActivity;
import com.gigya.android.sdk.tfa.workers.TokenUpdateWorker;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

/**
 * Main FCM messaging service.
 * Extend this service if your application already uses the FirebaseMessagingService.
 */
public class GigyaMessagingService extends FirebaseMessagingService {

    final private static String LOG_TAG = "GigyaMessagingService";

    public static final int PUSH_TFA_CONTENT_ACTION_REQUEST_CODE = 2020;
    public static final int PUSH_TFA_CONTENT_INTENT_REQUEST_CODE = 2021;

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

        // Content activity pending intent.
        Intent intent = new Intent(this, getCustomActionActivity());
        // We don't want the annoying enter animation.
        intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PUSH_TFA_CONTENT_INTENT_REQUEST_CODE,
                intent, PendingIntent.FLAG_ONE_SHOT);

        // Deny action.
        Intent denyIntent = new Intent(this, GigyaTFAActionReceiver.class);
        denyIntent.setAction(getString(R.string.tfa_action_deny));
        denyIntent.putExtra("notificationId", androidNotificationId);
        PendingIntent denyPendingIntent =
                PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, denyIntent, 0);

        // Approve action.
        Intent approveIntent = new Intent(this, GigyaTFAActionReceiver.class);
        approveIntent.setAction(getString(R.string.tfa_action_approve));
        approveIntent.putExtra("notificationId", androidNotificationId);
        PendingIntent approvePendingIntent =
                PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, approveIntent, 0);

        // Build notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.bg_gigya_custom)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(getDenyActionIcon(), getString(R.string.deny),
                        denyPendingIntent)
                .addAction(getApproveActionIcon(), getString(R.string.approve),
                        approvePendingIntent);

        // Notify.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(androidNotificationId, builder.build());
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

    //region CUSTOMIZATION OPTIONS

    /**
     * Optional override.
     * Define the notification approve action icon.
     *
     * @return Icon reference.
     */
    protected int getApproveActionIcon() {
        return 0;
    }

    /**
     * Optional override.
     * Define the notification deny action icon.
     *
     * @return Icon reference.
     */
    protected int getDenyActionIcon() {
        return 0;
    }

    /**
     * Optional override.
     * Allows to define the activity class used by the the notification's content intent.
     * default class GigyaPushTfaActivity.class.
     *
     * @return Activity class reference.
     */
    public Class getCustomActionActivity() {
        return GigyaPushTfaActivity.class;
    }

    //endregion
}
