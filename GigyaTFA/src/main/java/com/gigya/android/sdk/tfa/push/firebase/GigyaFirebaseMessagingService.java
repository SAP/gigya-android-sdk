package com.gigya.android.sdk.tfa.push.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.persistence.TFAPersistenceService;
import com.gigya.android.sdk.tfa.push.TFAPushReceiver;
import com.gigya.android.sdk.tfa.ui.PushTFAActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.PUSH_TFA_CONTENT_ACTION_REQUEST_CODE;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.PUSH_TFA_CONTENT_INTENT_REQUEST_CODE;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.PUSH_TFA_NOTIFICATION_ID;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

/**
 * Main FCM messaging service.
 * Extend this service if your application already uses the FirebaseMessagingService.
 */
public class GigyaFirebaseMessagingService extends FirebaseMessagingService {

    final private static String LOG_TAG = "GigyaMessagingService";

    @Override
    public void onCreate() {
        // Safe to call here. Once notification channel is created it won't be recreated again.
        createTFANotificationChannel();
    }

    private void createTFANotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.tfa_channel_name);
            String description = getString(R.string.tfa_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(TFA_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(String fcmToken) {
        GigyaLogger.debug(LOG_TAG, "onNewToken: " + fcmToken);

        if (fcmToken != null) {
            // Update push token in SDK preference file.
            new TFAPersistenceService(this).setPushToken(fcmToken);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        GigyaLogger.debug(LOG_TAG, "RemoteMessage: " + remoteMessage.toString());

        if (remoteMessage.getData().size() > 0) {
            // Check data purpose and continue flow accordingly.
            handleRemoteMessage(remoteMessage.getData());
        }
    }

    private void handleRemoteMessage(Map<String, String> data) {
        final String pushMode = data.get("mode");
        if (pushMode == null) {
            GigyaLogger.debug(LOG_TAG, "Push mode not available. Notification is ignored");
            return;
        }
        switch (pushMode) {
            case GigyaDefinitions.PushMode.OPT_IN:
            case GigyaDefinitions.PushMode.VERIFY:
                notifyWith(pushMode, data);
                break;
            case GigyaDefinitions.PushMode.CANCEL:
                cancel();
                break;
            default:
                GigyaLogger.debug(LOG_TAG, "Push mode not supported. Notification is ignored");
                break;
        }
    }

    private void notifyWith(String mode, Map<String, String> data) {
        // Fetch the data.
        final String title = data.get("title");
        final String body = data.get("body");
        final String gigyaAssertion = data.get("gigyaAssertion");
        final String verificationToken = data.get("verificationToken");

        GigyaLogger.debug(LOG_TAG, "verificationToken: " + verificationToken);

        // Content activity pending intent.
        Intent intent = new Intent(this, getCustomActionActivity());
        intent.putExtra("mode", mode);
        intent.putExtra("gigyaAssertion", gigyaAssertion);
        intent.putExtra("verificationToken", verificationToken);
        // We don't want the annoying enter animation.
        intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PUSH_TFA_CONTENT_INTENT_REQUEST_CODE,
                intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Deny action.
        Intent denyIntent = new Intent(this, TFAPushReceiver.class);
        denyIntent.putExtra("mode", mode);
        denyIntent.putExtra("gigyaAssertion", gigyaAssertion);
        denyIntent.putExtra("verificationToken", verificationToken);
        denyIntent.setAction(getString(R.string.tfa_action_deny));
        PendingIntent denyPendingIntent =
                PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, denyIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Approve action.
        Intent approveIntent = new Intent(this, TFAPushReceiver.class);
        approveIntent.putExtra("mode", mode);
        approveIntent.putExtra("gigyaAssertion", gigyaAssertion);
        approveIntent.putExtra("verificationToken", verificationToken);
        approveIntent.setAction(getString(R.string.tfa_action_approve));
        PendingIntent approvePendingIntent =
                PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, approveIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        // Build notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TFA_CHANNEL_ID)
                .setSmallIcon(getSmallIcon())
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(getDenyActionIcon(), getString(R.string.deny),
                        denyPendingIntent)
                .addAction(getApproveActionIcon(), getString(R.string.approve),
                        approvePendingIntent);

        // Notify.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(PUSH_TFA_NOTIFICATION_ID, builder.build());
    }

    /**
     * Attempt to cancel a displayed notification given a unique identification.
     */
    private void cancel() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(PUSH_TFA_NOTIFICATION_ID);
    }

    //region CUSTOMIZATION OPTIONS

    /**
     * Optional override
     * Define the notification small icon.
     *
     * @return Icon reference.
     */
    protected int getSmallIcon() {
        return android.R.drawable.ic_dialog_alert;
    }

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
        return PushTFAActivity.class;
    }

    //endregion
}
