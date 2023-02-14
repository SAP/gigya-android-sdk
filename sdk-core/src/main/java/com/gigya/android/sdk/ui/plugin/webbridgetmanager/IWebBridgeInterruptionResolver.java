package com.gigya.android.sdk.ui.plugin.webbridgetmanager;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;

import java.util.Map;

public interface IWebBridgeInterruptionResolver<A extends GigyaAccount> {

    boolean isActive();

    void resolve(@Nullable String api, Map<String, Object> params, A data, GigyaPluginFragment.IBridgeCallbacks<A> callback);
}
