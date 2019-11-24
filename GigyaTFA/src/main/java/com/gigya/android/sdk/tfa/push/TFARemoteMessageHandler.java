package com.gigya.android.sdk.tfa.push;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;
import com.gigya.android.sdk.push.RemoteMessageHandler;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.persistence.ITFAPersistenceService;

import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.PUSH_TFA_CONTENT_ACTION_REQUEST_CODE;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.PUSH_TFA_CONTENT_INTENT_REQUEST_CODE;
import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFA_CHANNEL_ID;

public class TFARemoteMessageHandler extends RemoteMessageHandler implements IRemoteMessageHandler {

    private static final String LOG_TAG = "TFARemoteMessageHandler";

    final private ITFAPersistenceService _psService;

    @Override
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _customizer = customizer;
    }

    public TFARemoteMessageHandler(Context context, ITFAPersistenceService psService, IGigyaNotificationManager gigyaNotificationManager) {
        super(context, gigyaNotificationManager, psService);
        _psService = psService;
    }

    @Override
    protected boolean remoteMessageMatchesHandlerContext(HashMap<String, String> remoteMessage) {
        return remoteMessage.containsKey("gigyaAssertion");
    }


    @Override
    public void handleRemoteMessage(@NonNull HashMap<String, String> remoteMessage) {

        if (!remoteMessageMatchesHandlerContext(remoteMessage)) {
            // This remote message is not relevant for this library instance.
            GigyaLogger.debug(LOG_TAG, "handleRemoteMessage: remote message not relevant for tfa service.");
            return;
        }

        /*
        Create/Update notification channel.
         */
        _gigyaNotificationManager.createNotificationChannelIfNeeded(_context,
                _context.getString(R.string.tfa_channel_name),
                _context.getString(R.string.tfa_channel_description),
                TFA_CHANNEL_ID
        );

        final String pushMode = remoteMessage.get("mode");
        if (pushMode == null) {
            GigyaLogger.debug(LOG_TAG, "Push mode not available. Notification is ignored");
            return;
        }

        switch (pushMode) {
            case com.gigya.android.sdk.GigyaDefinitions.PushMode.OPT_IN:
            case com.gigya.android.sdk.GigyaDefinitions.PushMode.VERIFY:
                notifyWith(pushMode, remoteMessage);
                break;
            case com.gigya.android.sdk.GigyaDefinitions.PushMode.CANCEL:
                cancel(remoteMessage);
                break;
            default:
                GigyaLogger.error(LOG_TAG, "Push mode not supported. Notification is ignored");
                break;
        }
    }

    @Override
    protected void notifyWith(String mode, HashMap<String, String> data) {

        // Parse notification fields.
        final String title = data.get("title");
        final String body = data.get("body");
        final String gigyaAssertion = data.get("gigyaAssertion");
        final String verificationToken = data.get("verificationToken");

        GigyaLogger.debug(LOG_TAG, "Action for vt: " + verificationToken);

        // the unique notification id will be the hash code of the gigyaAssertion field.
        int notificationId = 0;
        if (gigyaAssertion != null) {
            notificationId = Math.abs(gigyaAssertion.hashCode()); /* gigyaAssertion will act as notification id */
        }

        GigyaLogger.debug(LOG_TAG, "verificationToken: " + verificationToken);

        // Content activity pending intent.
        final Intent intent = new Intent(_context, _customizer.getCustomActionActivity());
        intent.putExtra("mode", mode);
        intent.putExtra("gigyaAssertion", gigyaAssertion);
        intent.putExtra("verificationToken", verificationToken);
        intent.putExtra("notificationId", notificationId);
        // We don't want the annoying enter animation.
        intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(_context, PUSH_TFA_CONTENT_INTENT_REQUEST_CODE,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Build notification.
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(_context, TFA_CHANNEL_ID)
                .setSmallIcon(_customizer.getSmallIcon())
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
            final Intent denyIntent = new Intent(_context, TFAPushReceiver.class);
            denyIntent.putExtra("mode", mode);
            denyIntent.putExtra("gigyaAssertion", gigyaAssertion);
            denyIntent.putExtra("verificationToken", verificationToken);
            denyIntent.putExtra("notificationId", notificationId);
            denyIntent.setAction(_context.getString(R.string.tfa_action_deny));
            final PendingIntent denyPendingIntent =
                    PendingIntent.getBroadcast(_context, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, denyIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            // Approve action.
            final Intent approveIntent = new Intent(_context, TFAPushReceiver.class);
            approveIntent.putExtra("mode", mode);
            approveIntent.putExtra("gigyaAssertion", gigyaAssertion);
            approveIntent.putExtra("verificationToken", verificationToken);
            approveIntent.putExtra("notificationId", notificationId);
            approveIntent.setAction(_context.getString(R.string.tfa_action_approve));
            final PendingIntent approvePendingIntent =
                    PendingIntent.getBroadcast(_context, PUSH_TFA_CONTENT_ACTION_REQUEST_CODE, approveIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            builder
                    .addAction(_customizer.getDenyActionIcon(), _context.getString(R.string.tfa_deny), denyPendingIntent)
                    .addAction(_customizer.getApproveActionIcon(), _context.getString(R.string.tfa_approve), approvePendingIntent);
        }

        // Notify.
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.notify(notificationId, builder.build());
    }

}
