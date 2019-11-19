package com.gigya.android.sdk.tfa.push;

import android.support.annotation.NonNull;

import java.util.HashMap;

public interface ITFARemoteMessageHandler {

    void handleRemoteMessage(@NonNull final HashMap<String, String> remoteMessage);

    void setPushCustomizer(TFAPushCustomizer customizer);
}
