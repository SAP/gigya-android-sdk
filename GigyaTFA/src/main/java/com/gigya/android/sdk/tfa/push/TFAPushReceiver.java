package com.gigya.android.sdk.tfa.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.tfa.GigyaTFA;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.utils.ObjectUtils;

public class TFAPushReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "GigyaTFAActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

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

        switch (mode) {
            case GigyaDefinitions.PushMode.OPT_IN:
            case GigyaDefinitions.PushMode.VERIFY:
                if (isDenyAction(context, action)) {
                    // Redundant.
                    GigyaLogger.debug(LOG_TAG, "Opt-In mode. User chose to deny. Flow will not complete.");
                } else if (isApproveAction(context, action)) {
                    GigyaLogger.debug(LOG_TAG, "Opt-In mode. User chose to approve. Complete Opt-In flow.");

                    // Fetch tokens.
                    final String gigyaAssertion = intent.getStringExtra("gigyaAssertion");
                    final String verificationToken = intent.getStringExtra("verificationToken");

                    GigyaLogger.debug(LOG_TAG, "Action for vt: " + verificationToken);

                    // Continue flow.
                    if (mode.equals(GigyaDefinitions.PushMode.OPT_IN)) {
                        GigyaTFA.getInstance().verifyOptInForPushTFA(gigyaAssertion, verificationToken);
                    } else {
                        GigyaTFA.getInstance().approveLoginForPushTFA(gigyaAssertion, verificationToken);
                    }
                }
                break;
            default:
                GigyaLogger.error(LOG_TAG, "Push mode not supported. Action ignored. Flow is broken");
                break;
        }
    }

    private boolean isDenyAction(Context context, String action) {
        return ObjectUtils.safeEquals(action, context.getString(R.string.tfa_action_deny));
    }

    private boolean isApproveAction(Context context, String action) {
        return ObjectUtils.safeEquals(action, context.getString(R.string.tfa_action_approve));
    }

}
