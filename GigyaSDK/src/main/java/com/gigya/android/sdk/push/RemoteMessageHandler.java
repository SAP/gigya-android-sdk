package com.gigya.android.sdk.push;

import android.content.Context;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.persistence.IPersistenceService;

import java.util.HashMap;

public abstract class RemoteMessageHandler implements IRemoteMessageHandler {

    final protected Context _context;

    final protected IGigyaNotificationManager _gigyaNotificationManager;

    final protected IPersistenceService _persistenceService;

    protected IGigyaPushCustomizer _customizer;

    private static final String LOG_TAG = "GigyaRemoteMessageHandler";

    protected RemoteMessageHandler(Context context,IGigyaNotificationManager gigyaNotificationManager, IPersistenceService persistenceService) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
        _persistenceService = persistenceService;
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

    /**
     * Check if the current session is encrypted using a biometric key.
     */
    protected boolean isDefaultEncryptedSession() {
        return _persistenceService.getSessionEncryptionType().equals(com.gigya.android.sdk.GigyaDefinitions.SessionEncryption.DEFAULT);
    }

}
