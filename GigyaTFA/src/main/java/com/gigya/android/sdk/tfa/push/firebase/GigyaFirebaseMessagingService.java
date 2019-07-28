package com.gigya.android.sdk.tfa.push.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.GigyaTFA;
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
import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

/**
 * Main FCM messaging service.
 * Extend this service if your application already uses the FirebaseMessagingService.
 */
public class GigyaFirebaseMessagingService extends FirebaseMessagingService {

    final private static String LOG_TAG = "GigyaMessagingService";

    private TFAPersistenceService _psService;

    @Override
    public void onCreate() {
        // Safe to call here. Once notification channel is created it won't be recreated again.
        createTFANotificationChannel();

        GigyaLogger.info(LOG_TAG, "GigyaFirebaseMessagingService created: If you have not extended this service with" +
                " your own service class, all notification with use small icon \"android.R.drawable.ic_dialog_in\'");

        // Instantiate new persistence service extension.
        _psService = new TFAPersistenceService(this);
    }

    private void createTFANotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final CharSequence name = getString(R.string.tfa_channel_name);
            final String description = getString(R.string.tfa_channel_description);
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            final NotificationChannel channel = new NotificationChannel(TFA_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(String newToken) {
        GigyaLogger.debug(LOG_TAG, "onNewToken: " + newToken);

        if (newToken != null) {
            // Check for token updates.
            final String persistentToken = _psService.getPushToken();
            if (persistentToken == null) {
                // Update push token in SDK preference file.
                _psService.setPushToken(newToken);
                return;
            }

            if (!persistentToken.equals(newToken)) {
                // Push token for this device has been updated.
                GigyaLogger.debug(LOG_TAG, "Firebase token recycled. New token needs to be updated in server");
                GigyaTFA.getInstance().updateDeviceInfoForPushTFA(newToken);
            }
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

    /**
     * Handle push message data.
     * Fetch "mode" property from data and apply logic accordingly.
     *
     * @param data Notification data map.
     */
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
                cancel(data);
                break;
            default:
                GigyaLogger.debug(LOG_TAG, "Push mode not supported. Notification is ignored");
                break;
        }
    }

    /**
     * Check if the current session is encrypted using a biometric key.
     */
    private boolean isDefaultEncryptedSession() {
        return _psService.getSessionEncryptionType().equals(com.gigya.android.sdk.GigyaDefinitions.SessionEncryption.DEFAULT);
    }

    private void notifyWith(String mode, Map<String, String> data) {
        // Fetch the data.
        final String title = data.get("title");
        final String body = data.get("body");
        final String gigyaAssertion = data.get("gigyaAssertion");
        final String verificationToken = data.get("verificationToken");

        GigyaLogger.debug(LOG_TAG, "Action for vt: " + verificationToken);

        // the unique notification id will be the hash code of the gigyaAssertion field.
        int notificationId = 0;
        if (gigyaAssertion != null) {
            notificationId = Math.abs(gigyaAssertion.hashCode());
        }

        GigyaLogger.debug(LOG_TAG, "verificationToken: " + verificationToken);

        // Content activity pending intent.
        final Intent intent = new Intent(this, getCustomActionActivity());
        intent.putExtra("mode", mode);
        intent.putExtra("gigyaAssertion", gigyaAssertion);
        intent.putExtra("verificationToken", verificationToken);
        intent.putExtra("notificationId", notificationId);
        // We don't want the annoying enter animation.
        intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, PUSH_TFA_CONTENT_INTENT_REQUEST_CODE,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Build notification.
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TFA_CHANNEL_ID)
                .setSmallIcon(getSmallIcon())
                .setContentTitle(title != null ? title.trim() : "")
                .setContentText(body != null ? body.trim() : "")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Adding notification actions only for default encrypted sessions.
        // This is due to the fact that decrypting a session is a time consuming task and performing it via a
        // broadcast receiver context can cause intent data to be flushed before usage.
        if (isDefaultEncryptedSession()) {
            // Deny action.
            final Intent denyIntent = new Intent(this, TFAPushReceiver.class);
            denyIntent.putExtra("mode", mode);
            denyIntent.putExtra("gigyaAssertion", gigyaAssertion);
            denyIntent.putExtra("verificationToken", verificationToken);
            denyIntent.putExtra("notificationId", notificationId);
            denyIntent.setAction(getString(R.string.tfa_action_deny));
            final PendingIntent denyPendingIntent =
                    PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, denyIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            // Approve action.
            final Intent approveIntent = new Intent(this, TFAPushReceiver.class);
            approveIntent.putExtra("mode", mode);
            approveIntent.putExtra("gigyaAssertion", gigyaAssertion);
            approveIntent.putExtra("verificationToken", verificationToken);
            approveIntent.putExtra("notificationId", notificationId);
            approveIntent.setAction(getString(R.string.tfa_action_approve));
            final PendingIntent approvePendingIntent =
                    PendingIntent.getBroadcast(this, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, approveIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            builder
                    .addAction(getDenyActionIcon(), getString(R.string.tfa_deny), denyPendingIntent)
                    .addAction(getApproveActionIcon(), getString(R.string.tfa_approve), approvePendingIntent);
        }


        // Apply full customization options (not in v 1.0.0).
        if (getStyle() != null) {
            builder.setStyle(getStyle());
        }
        if (getNotificationLayout() != null) {
            builder.setCustomContentView(getNotificationLayout());
        }
        if (getNotificationLayoutExtended() != null) {
            /*
            To support Android versions older than Android 4.1 (API level 16), you should also call setContent(), passing it the same RemoteViews object.
            @see <a href="https://developer.android.com/training/notify-user/custom-notification"></a>
             */
            builder.setCustomBigContentView(getNotificationLayoutExtended());
            builder.setContent(getNotificationLayoutExtended());
        }

        // Notify.
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Attempt to cancel a displayed notification given a unique identification.
     */
    private void cancel(Map<String, String> data) {
        final String gigyaAssertion = data.get("gigyaAssertion");
        int notificationId = 0;
        if (gigyaAssertion != null) {
            notificationId = Math.abs(gigyaAssertion.hashCode());
        }
        GigyaLogger.debug(LOG_TAG, "Cancel push received. Cancelling push approval notification");
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(notificationId);
    }

    //region CUSTOMIZATION OPTIONS

    /**
     * Optional override
     * Define the notification small icon.
     *
     * @return Icon reference.
     */
    protected int getSmallIcon() {
        return android.R.drawable.ic_dialog_info;
    }

    /**
     * Optional override
     * Define a custom notification style.
     *
     * @return Customized style instance.
     */
    private NotificationCompat.Style getStyle() {
        return null;
    }

    /**
     * Optional override
     * Define a custom notification layout.
     * not available in version 1.0.0.
     *
     * @return RemoteViews instance.
     */
    private RemoteViews getNotificationLayout() {
        return null;
    }

    /*
    Not available in 1.0.0
     */
    private RemoteViews getNotificationLayoutExtended() {
        return null;
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
    protected Class getCustomActionActivity() {
        return PushTFAActivity.class;
    }

    //endregion
}
