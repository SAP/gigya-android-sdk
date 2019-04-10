package com.gigya.android.sdk.ui.plugin;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;

public class WebBridgeFactory<T extends GigyaAccount> implements IWebBridgeFactory<T> {

    final private Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IBusinessApiService<T> _bApiService;
    final private IProviderFactory _providerFactory;

    public WebBridgeFactory(Context context, Config config, ISessionService sessionService, IAccountService accountService,
                            IBusinessApiService<T> bApiService, IProviderFactory providerFactory) {

        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _bApiService = bApiService;
        _providerFactory = providerFactory;
    }

    @Override
    public WebBridge create(boolean obfuscate, IWebBridge<T> wbInteractions) {
        return new WebBridge<>(_context, _config, _sessionService, _accountService, _bApiService, _providerFactory, obfuscate, wbInteractions);
    }
}
