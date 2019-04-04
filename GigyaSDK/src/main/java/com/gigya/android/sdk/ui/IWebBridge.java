package com.gigya.android.sdk.ui;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;

public interface IWebBridge<T extends GigyaAccount> {

    void onPluginEvent(GigyaPluginEvent event, String containerID);

    void onAuthEvent(WebBridge.AuthEvent authEvent, T obj);

    void onCancel();

    void onError(GigyaError error);
}
