package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;

import java.util.Map;

public interface IWebBridgeInterruptionManager<A extends GigyaAccount> {

    void responseManager(Map<String, Object> params, A data, GigyaPluginFragment.IBridgeCallbacks<A> callback);

    void interruptionHandler(GigyaError error);
}
