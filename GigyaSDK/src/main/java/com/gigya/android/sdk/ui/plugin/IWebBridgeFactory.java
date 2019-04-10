package com.gigya.android.sdk.ui.plugin;

import com.gigya.android.sdk.model.account.GigyaAccount;

public interface IWebBridgeFactory<T extends GigyaAccount> {

    WebBridge create(boolean obfuscate, IWebBridge<T> wbInteractions);
}
