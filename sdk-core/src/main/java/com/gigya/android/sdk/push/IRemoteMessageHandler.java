package com.gigya.android.sdk.push;

import androidx.annotation.NonNull;

import java.util.HashMap;

public interface IRemoteMessageHandler {

    void handleRemoteMessage(@NonNull final HashMap<String, String> remoteMessage);

    void setPushCustomizer(IGigyaPushCustomizer customizer);
}
