package com.gigya.android.sdk.ui.plugin;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.session.ISessionService;

public class WebBridgeFactory<T extends GigyaAccount> implements IWebBridgeFactory<T> {

    final private Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IBusinessApiService<T> _bApiService;

    public WebBridgeFactory(Context context, Config config, ISessionService sessionService, IAccountService accountService,
                            IBusinessApiService<T> bApiService) {

        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _bApiService = bApiService;
    }

    @Override
    public WebBridge create(boolean obfuscate, IWebBridge<T> wbInteractions) {
        return new WebBridge<>(_context, _config, _sessionService, _accountService, _bApiService, obfuscate, wbInteractions);
    }
}
