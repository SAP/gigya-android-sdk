package com.gigya.android.sdk.tfa.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.push.GigyaFirebaseMessagingService;

import java.util.HashMap;

public class TFARemoteMessageLocalReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TFARemoteMessageLocalReceiver";

    final private ITFARemoteMessageHandler _messageHandler;

    public TFARemoteMessageLocalReceiver(ITFARemoteMessageHandler messageHandler) {
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

        if (remoteMessage.containsKey("gigyaAssertion")) {
            GigyaLogger.error(LOG_TAG, "onReceive: TFA related remote message");
            _messageHandler.handleRemoteMessage(remoteMessage);
        }

    }

}
