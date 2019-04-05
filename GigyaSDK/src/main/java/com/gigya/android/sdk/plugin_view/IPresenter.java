package com.gigya.android.sdk.plugin_view;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.GigyaPluginCallback;

import java.util.List;
import java.util.Map;

public interface IPresenter {

    void showPlugin(boolean obfuscate, final String plugin, final Map<String, Object> params, final GigyaPluginCallback callback);

    void showNativeLoginProviders(@GigyaDefinitions.Providers.SocialProvider List<String> providers, Map<String, Object> params, GigyaLoginCallback gigyaLoginCallback);

    String getPresentationUrl(Map<String, Object> params, String requestType);

}
