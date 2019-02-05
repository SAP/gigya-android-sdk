package com.gigya.android.sdk;

import com.gigya.android.sdk.network.GigyaError;

import java.util.Map;

public abstract class GigyaPluginCallback<T> {

    public void onLogin(T obj) {
        // Stub.
    }

    public void onLogout() {
        // Stub.
    }

    public abstract void onEvent(String eventName, Map<String, Object> parameters);

    public abstract void onCancel();

    public abstract void onError(GigyaError error);
}
