package com.gigya.android.sdk.providers;

import android.content.Context;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.managers.IAccountService;
import com.gigya.android.sdk.managers.IApiService;
import com.gigya.android.sdk.managers.ISessionService;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.services.Config;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.LINE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

public class ProviderFactory implements IProviderFactory {

    final private Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IApiService _apiService;
    final private IPersistenceService _psService;

    public ProviderFactory(Context context, Config config, ISessionService sessionService, IAccountService accountService,
                           IApiService apiService, IPersistenceService persistenceService) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _psService = persistenceService;
    }

    @Override
    public Provider providerFor(String name, GigyaLoginCallback gigyaLoginCallback) {
        switch (name.toLowerCase()) {
            case FACEBOOK:
                if (FacebookProvider.isAvailable(_context)) {
                    return new FacebookProvider(_config, _sessionService, _accountService, _apiService, _psService, gigyaLoginCallback);
                }
                break;
            case GOOGLE:
                if (GoogleProvider.isAvailable(_context)) {
                    return new GoogleProvider(_config, _sessionService, _accountService, _apiService, _psService, gigyaLoginCallback);
                }
                break;
            case LINE:
                if (LineProvider.isAvailable(_context)) {
                    return new LineProvider(_config, _sessionService, _accountService, _apiService, _psService, gigyaLoginCallback);
                }
                break;
            case WECHAT:
                if (WeChatProvider.isAvailable(_context)) {
                    return new WeChatProvider(_config, _sessionService, _accountService, _apiService, _psService, gigyaLoginCallback);
                }
                break;
        }
        return new WebViewProvider(_config, _sessionService, _accountService, _apiService, _psService, gigyaLoginCallback);
    }
}
