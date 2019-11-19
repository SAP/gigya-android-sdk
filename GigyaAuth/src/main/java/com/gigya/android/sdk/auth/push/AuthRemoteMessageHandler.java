package com.gigya.android.sdk.auth.push;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaPushCustomizer;
import com.gigya.android.sdk.push.IRemoteMessageHandler;

import java.util.HashMap;

public class AuthRemoteMessageHandler implements IRemoteMessageHandler {

    private static final String LOG_TAG = "AuthRemoteMessageHandler";

    final private Context _context;
    final private IGigyaNotificationManager _gigyaNotificationManager;

    private IGigyaPushCustomizer _customizer;

    @Override
    public void setPushCustomizer(IGigyaPushCustomizer customizer) {
        _customizer = customizer;
    }

    public AuthRemoteMessageHandler(Context context, IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;
    }


    @Override
    public void handleRemoteMessage(@NonNull HashMap<String, String> remoteMessage) {


    }

}
