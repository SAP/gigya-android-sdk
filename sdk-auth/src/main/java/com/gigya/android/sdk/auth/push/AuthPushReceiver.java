package com.gigya.android.sdk.auth.push;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.GigyaAuth;
import com.gigya.android.sdk.auth.R;
import com.gigya.android.sdk.utils.ObjectUtils;

public class AuthPushReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "AuthPushReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getExtras() == null) {
            return;
        }

        // Make sure to cancel the notification.
        final int notificationId = intent.getIntExtra("notificationId", 0);
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);

        // Fetch push mode from intent extras. Mandatory.
        final String mode = intent.getStringExtra("mode");
        if (mode == null) {
            GigyaLogger.error(LOG_TAG, "Push mode not available. Action ignored. Flow is broken");
            return;
        }

        // Fetch intent action from intent extras. Mandatory.
        final String action = intent.getAction();
        if (action == null) {
            GigyaLogger.error(LOG_TAG, "Action not available. Action ignored. Flow is broken");
            return;
        }

        GigyaLogger.debug(LOG_TAG, "onReceive action: " + action);

        if (GigyaDefinitions.PushMode.VERIFY.equals(mode)) {
            if (isDenyAction(context, action)) {

                // Redundant.
                GigyaLogger.debug(LOG_TAG, "onReceive: deny action will dismiss the flow.");
            } else if (isApproveAction(context, action)) {

                GigyaLogger.debug(LOG_TAG, "onReceive: approve action selected.");

                final String verificationToken = intent.getStringExtra("vToken");
                if (verificationToken == null) {
                    GigyaLogger.error(LOG_TAG, "onReceive: failed to parse verification token");
                    return;
                }

                /*
                Call verification action.
                */
                GigyaAuth.getInstance().verifyAuthPush(verificationToken);
            }
        } else {
            GigyaLogger.error(LOG_TAG, "Push mode not supported. Action ignored. Flow is broken");
        }
    }

    private boolean isDenyAction(Context context, String action) {
        return ObjectUtils.safeEquals(action, context.getString(R.string.gig_auth_action_deny));
    }

    private boolean isApproveAction(Context context, String action) {
        return ObjectUtils.safeEquals(action, context.getString(R.string.gig_auth_action_approve));
    }
}
