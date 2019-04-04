package com.gigya.android.sdk.plugin_view;

import com.gigya.android.sdk.managers.IApiService;
import com.gigya.android.sdk.managers.ISessionService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.ui.IWebBridge;
import com.gigya.android.sdk.ui.WebBridge;

public class WebBridgeFactory implements IWebBridgeFactory {

    final private Config _config;
    final private ISessionService _sessionService;
    final private IApiService _apiService;

    public WebBridgeFactory(Config config, ISessionService sessionService, IApiService apiService) {
        _config = config;
        _sessionService = sessionService;
        _apiService = apiService;
    }

    @Override
    public WebBridge create(boolean obfuscate, IWebBridge wbInteractions) {
        return new WebBridge(_config, _sessionService, _apiService, obfuscate, wbInteractions);
    }
}
