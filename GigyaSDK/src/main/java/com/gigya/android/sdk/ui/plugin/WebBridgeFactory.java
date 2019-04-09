package com.gigya.android.sdk.ui.plugin;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;

public class WebBridgeFactory implements IWebBridgeFactory {

    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IApiService _apiService;
    final private IProviderFactory _providerFactory;

    public WebBridgeFactory(Config config, ISessionService sessionService, IAccountService accountService, IApiService apiService, IProviderFactory providerFactory) {
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _providerFactory = providerFactory;
    }

    @Override
    public WebBridge create(boolean obfuscate, IWebBridge wbInteractions) {
        return new WebBridge(_config, _sessionService, _accountService, _apiService, _providerFactory, obfuscate, wbInteractions);
    }
}
