package com.gigya.android.sdk.push;

import android.content.Context;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;

import java.util.HashMap;

public abstract class RemoteMessageHandler implements IRemoteMessageHandler {

    final protected Context _context;

    final protected IGigyaNotificationManager _gigyaNotificationManager;

    protected IGigyaPushCustomizer _customizer;

    private static final String LOG_TAG = "GigyaRemoteMessageHandler";

    protected RemoteMessageHandler(Context context,IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
    }

    protected abstract boolean remoteMessageMatchesHandlerContext(HashMap<String, String> remoteMessage);

    protected abstract void notifyWith(String mode, HashMap<String, String> data);

    /**
     * Attempt to cancel a displayed notification given a unique identification.
     */
    protected void cancel(HashMap<String, String> data) {
        final String gigyaAssertion = data.get("gigyaAssertion");
        int notificationId = 0;
        if (gigyaAssertion != null) {
            notificationId = Math.abs(gigyaAssertion.hashCode());
        }
        GigyaLogger.debug(LOG_TAG, "Cancel push received. Cancelling push approval notification");
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.cancel(notificationId);
    }

}
