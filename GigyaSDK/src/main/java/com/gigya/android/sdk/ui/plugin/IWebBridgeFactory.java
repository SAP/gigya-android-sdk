package com.gigya.android.sdk.ui.plugin;

import com.gigya.android.sdk.account.models.GigyaAccount;

public interface IWebBridgeFactory<T extends GigyaAccount> {

    WebBridge create(boolean obfuscate, IWebBridge<T> wbInteractions);
}
