package com.gigya.android.sdk.auth.push;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.push.RemoteMessageHandler;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;

import java.util.HashMap;

public class AuthRemoteMessageHandler extends RemoteMessageHandler implements IRemoteMessageHandler {

    private static final String LOG_TAG = "AuthRemoteMessageHandler";

    final private IGigyaNotificationManager _gigyaNotificationManager;

    private IGigyaPushCustomizer _customizer;

    @Override
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _customizer = customizer;
    }

    public AuthRemoteMessageHandler(Context context, IGigyaNotificationManager gigyaNotificationManager) {
        super(context);
        _gigyaNotificationManager = gigyaNotificationManager;
    }

    @Override
    public void handleRemoteMessage(@NonNull HashMap<String, String> remoteMessage) {

        if (!remoteMessage.containsKey("AuthChallenge")) {
            GigyaLogger.debug(LOG_TAG, "handleRemoteMessage: remote message not relevant for auth service.");
            return;
        }

        final String pushMode = remoteMessage.get("mode");
        if (pushMode == null) {
            GigyaLogger.debug(LOG_TAG, "Push mode not available. Notification is ignored");
            return;
        }

        switch (pushMode) {
            case GigyaDefinitions.PushMode.CANCEL:
                cancel(remoteMessage);
                break;
            case GigyaDefinitions.PushMode.VERIFY:
                notifyWith(pushMode, remoteMessage);
                break;
            default:
                // Unhandled.
                break;
        }
    }

    @Override
    protected void notifyWith(String mode, HashMap<String, String> data) {

    }
}
