package com.gigya.android.sdk.push;

import android.content.Context;
import android.support.v4.app.NotificationManagerCompat;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;

public abstract class RemoteMessageHandler implements IRemoteMessageHandler {

    protected final Context _context;

    protected final IGigyaNotificationManager _gigyaNotificationManager;

    protected final IPersistenceService _persistenceService;

    protected final ISessionService _sessionService;

    protected IGigyaPushCustomizer _customizer;

    private static final String LOG_TAG = "GigyaRemoteMessageHandler";

    protected RemoteMessageHandler(Context context, ISessionService sessionService, IGigyaNotificationManager gigyaNotificationManager, IPersistenceService persistenceService) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
        _persistenceService = persistenceService;
        _sessionService = sessionService;
    }

    protected abstract boolean remoteMessageMatchesHandlerContext(HashMap<String, String> remoteMessage);

    protected abstract void notifyWith(String mode, HashMap<String, String> data);

    /**
     * Attempt to cancel a displayed notification given a unique identification.
     */
    protected void cancel(int notificationId) {
        if (notificationId == 0) {
            return;
        }
        GigyaLogger.debug(LOG_TAG, "Cancel notification with id = " + notificationId);
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.cancel(notificationId);
    }

    /**
     * Check if the current session is encrypted using a biometric key.
     */
    protected boolean isDefaultEncryptedSession() {
        return _persistenceService.getSessionEncryptionType().equals(com.gigya.android.sdk.GigyaDefinitions.SessionEncryption.DEFAULT);
    }

    protected boolean isSessionValidForRemoteNotifications() {
        return _sessionService.isValid();
    }

}
