package com.gigya.android.sdk.ui.new_plugin_impl;

import java.util.Map;

public interface IGigyaWebBridge {

    boolean invoke(String action, String method, String queryStringParams);

    boolean invoke(String url);

    void invokeCallback(String id, String baseInvocation);

    void getIds(String id);

    void isSessionValid(String id);

    void sendRequest(final String callbackId, final String api, Map<String, Object> params, Map<String, Object> settings);

    void sendOAuthRequest(final String callbackId, String api, Map<String, Object> params, Map<String, Object> settings);

    void onPluginEvent(Map<String, Object> params);

}
