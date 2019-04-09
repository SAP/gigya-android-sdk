package com.gigya.android.sdk.interruption;

import android.content.Context;
import android.support.v4.util.Pair;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.model.account.ConflictingAccounts;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GigyaLinkAccountsResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    private static final String LOG_TAG = "GigyaLinkAccountsResolver";

    private ConflictingAccounts conflictingAccounts;
    private IProviderFactory _providerFactory;

    public GigyaLinkAccountsResolver(IProviderFactory providerFactory,
                                     IApiService apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super(apiService, originalResponse, loginCallback);
        _providerFactory = providerFactory;
    }

    public ConflictingAccounts getConflictingAccounts() {
        return conflictingAccounts;
    }

    @Override
    public void init() {
        GigyaLogger.debug(LOG_TAG, "init: sending fetching conflicting accounts");
        // Get conflicting accounts.
        _apiService.send(GigyaDefinitions.API.API_GET_CONFLICTING_ACCOUNTS,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("regToken", _regToken))),
                RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse obj) {
                        GigyaLinkAccountsResolver.this.conflictingAccounts = obj.getField("conflictingAccount", ConflictingAccounts.class);
                        if (GigyaLinkAccountsResolver.this.conflictingAccounts == null) {
                            forwardError(GigyaError.generalError());
                        } else {
                            if (isAttached()) {
                                _loginCallback.get().onConflictingAccounts(_originalResponse, GigyaLinkAccountsResolver.this);
                            }
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                }
        );
    }

    @Override
    public void clear() {
        _regToken = null;
        this.conflictingAccounts = null;
        if (isAttached()) {
            _loginCallback.clear();
        }
    }

    public void resolveForSite(String loginID, String password) {
        if (isAttached()) {
            _apiService.login(ObjectUtils.mapOf(Arrays.asList(
                    new Pair<String, Object>("loginID", loginID),
                    new Pair<String, Object>("password", password),
                    new Pair<String, Object>("loginMode", "link"),
                    new Pair<String, Object>("regToken", _regToken))),
                    _loginCallback.get());
        }
    }

    public void resolveForSocial(final Context context, String providerName) {
        if (isAttached()) {
            Provider provider = _providerFactory.providerFor(providerName, _loginCallback.get());
            provider.setRegToken(_regToken);
            Map<String, Object> params = new HashMap<>();
            params.put("provider", provider);
            provider.login(context, params, "link");
        }
    }


    void finalizeFlow(String regToken) {
        if (isAttached()) {
            _apiService.finalizeRegistration(regToken, _loginCallback.get());
        }
    }
}
