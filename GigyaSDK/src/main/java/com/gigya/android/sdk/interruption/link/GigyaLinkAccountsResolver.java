package com.gigya.android.sdk.interruption.link;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.account.ConflictingAccounts;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

public class GigyaLinkAccountsResolver<A extends GigyaAccount> extends GigyaResolver<A> implements IGigyaLinkAccountsResolver {

    private static final String LOG_TAG = "GigyaLinkAccountsResolver";

    private ConflictingAccounts conflictingAccounts;
    private IProviderFactory _providerFactory;

    public GigyaLinkAccountsResolver(Config config, ISessionService sessionService, IProviderFactory providerFactory,
                                     IApiService apiService, IApiObservable observable, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super(config, sessionService, apiService, observable, originalResponse, loginCallback);
        _providerFactory = providerFactory;
    }

    @Override
    public ConflictingAccounts getConflictingAccounts() {
        return conflictingAccounts;
    }

    @Override
    public void start() {
        GigyaLogger.debug(LOG_TAG, "init: sending fetching conflicting accounts");
        // Get conflicting accounts.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", _regToken);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_GET_CONFLICTING_ACCOUNTS, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    GigyaLinkAccountsResolver.this.conflictingAccounts = response.getField("conflictingAccount", ConflictingAccounts.class);
                    if (GigyaLinkAccountsResolver.this.conflictingAccounts == null) {
                        forwardError(GigyaError.generalError());
                    } else {
                        if (isAttached()) {
                            _loginCallback.get().onConflictingAccounts(_originalResponse, GigyaLinkAccountsResolver.this);
                        }
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    @Override
    public void clear() {
        _regToken = null;
        this.conflictingAccounts = null;
        if (isAttached()) {
            _loginCallback.clear();
        }
    }

    @Override
    public void linkToSite(String loginID, String password) {
        if (isAttached()) {
            final Map<String, Object> params = new HashMap<>();
            params.put("loginID", loginID);
            params.put("password", password);
            params.put("loginMode", "link");
            params.put("regToken", _regToken);
            final String api = GigyaDefinitions.API.API_LOGIN;
            _observable.send(api, params, _loginCallback.get());
        }
    }

    @Override
    public void linkToSocial(String providerName) {
        if (isAttached()) {
            IProvider provider = _providerFactory.providerFor(providerName, _observable, _loginCallback.get());
            provider.setRegToken(_regToken);
            Map<String, Object> params = new HashMap<>();
            params.put("provider", provider);
            provider.login(params, "link");
        }
    }
}
