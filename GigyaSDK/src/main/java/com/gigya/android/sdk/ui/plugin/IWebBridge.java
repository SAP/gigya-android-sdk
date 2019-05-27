package com.gigya.android.sdk.ui.plugin;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;

public interface IWebBridge<T extends GigyaAccount> {

    void onPluginEvent(GigyaPluginEvent event, String containerID);

    void onAuthEvent(WebBridge.AuthEvent authEvent, T obj);

    void onCancel();

    void onError(GigyaError error);
}
