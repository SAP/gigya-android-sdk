package com.gigya.android.sdk.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gigya.android.sdk.GigyaLogger;

import java.util.HashMap;

public class RemoteMessageLocalReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TFARemoteMessageLocalReceiver";

    final private IRemoteMessageHandler _messageHandler;

    public RemoteMessageLocalReceiver(IRemoteMessageHandler messageHandler) {
        _messageHandler = messageHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() == null) {
            GigyaLogger.error(LOG_TAG, "onReceive: extras null!");
            return;
        }

        @SuppressWarnings("unchecked") final HashMap<String, String> remoteMessage = (HashMap<String, String>) intent.getExtras().get(GigyaFirebaseMessagingService.EXTRA_REMOTE_MESSAGE_DATA);
        if (remoteMessage == null) {
            GigyaLogger.error(LOG_TAG, "onReceive: remoteMessage null!");
            return;
        }

        _messageHandler.handleRemoteMessage(remoteMessage);
    }

}
