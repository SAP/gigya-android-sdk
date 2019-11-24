package com.gigya.android.sdk.auth.push;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;
import com.gigya.android.sdk.push.RemoteMessageHandler;
import com.gigya.android.sdk.tfa.R;

import java.util.HashMap;

import static com.gigya.android.sdk.auth.GigyaDefinitions.AUTH_CHANNEL_ID;

public class AuthRemoteMessageHandler extends RemoteMessageHandler implements IRemoteMessageHandler {

    private static final String LOG_TAG = "AuthRemoteMessageHandler";

    @Override
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _customizer = customizer;
    }

    public AuthRemoteMessageHandler(Context context, IGigyaNotificationManager gigyaNotificationManager) {
        super(context, gigyaNotificationManager);
    }

    @Override
    protected boolean remoteMessageMatchesHandlerContext(HashMap<String, String> remoteMessage) {
        return remoteMessage.containsKey("AuthChallenge");
    }

    @Override
    public void handleRemoteMessage(@NonNull HashMap<String, String> remoteMessage) {

        if (!remoteMessageMatchesHandlerContext(remoteMessage)) {
            GigyaLogger.debug(LOG_TAG, "handleRemoteMessage: remote message not relevant for auth service.");
            return;
        }

        /*
        Create/Update notification channel.
         */
        _gigyaNotificationManager.createNotificationChannelIfNeeded(_context,
                _context.getString(R.string.auth_channel_name),
                _context.getString(R.string.auth_channel_description),
                AUTH_CHANNEL_ID
        );

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
