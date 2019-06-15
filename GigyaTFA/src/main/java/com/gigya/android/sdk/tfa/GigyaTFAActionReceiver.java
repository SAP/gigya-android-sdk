package com.gigya.android.sdk.tfa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.utils.ObjectUtils;

public class GigyaTFAActionReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "GigyaTFAActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        GigyaLogger.debug(LOG_TAG, "onReceive action: " + action);

        // Make sure to cancel the notification.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        final int notificationId = intent.getIntExtra("notificationId", 0);
        if (notificationId == 0) {
            notificationManager.cancelAll();
        } else {
            notificationManager.cancel(notificationId);
        }

        // Evaluate action.
        if (ObjectUtils.safeEquals(action, context.getString(R.string.tfa_action_deny))) {
            GigyaLogger.debug(LOG_TAG, "onReceive deny action chosen");
            GigyaTFA.getInstance().onDenyPushTFA();
        } else if (ObjectUtils.safeEquals(action, context.getString(R.string.tfa_action_approve))) {
            GigyaLogger.debug(LOG_TAG, "onReceive approve action chosen");
            GigyaTFA.getInstance().onApprovePushTFA();
        }
    }

}
