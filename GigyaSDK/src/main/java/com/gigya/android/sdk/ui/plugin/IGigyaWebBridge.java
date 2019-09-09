package com.gigya.android.sdk.ui.plugin;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;

import java.util.Map;

public interface IGigyaWebBridge<A extends GigyaAccount> {

    void withObfuscation(boolean obfuscation);

    void setInvocationCallback(@NonNull GigyaPluginFragment.IBridgeCallbacks<A> invocationCallback);

    boolean invoke(String action, String method, String queryStringParams);

    boolean invoke(String url);

    void invokeWebViewCallback(String id, String baseInvocation);

    void getIds(String id);

    void isSessionValid(String id);

    void sendRequest(final String callbackId, final String api, Map<String, Object> params, Map<String, Object> settings);

    void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params, Map<String, Object> settings);

    void onPluginEvent(Map<String, Object> params);

    void attachTo(@NonNull final WebView webView,
                  @NonNull final GigyaPluginCallback<A> pluginCallback,
                  @Nullable View progressView);

    void detachFrom(@NonNull final WebView webView);

}
