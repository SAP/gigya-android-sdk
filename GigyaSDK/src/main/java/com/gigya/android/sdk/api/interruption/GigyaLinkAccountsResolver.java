package com.gigya.android.sdk.api.interruption;

import android.content.Context;
import android.support.v4.util.Pair;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.ConflictingAccounts;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.providers.LoginProvider;
import com.gigya.android.sdk.providers.LoginProviderFactory;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GigyaLinkAccountsResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    private static final String LOG_TAG = "GigyaLinkAccountsResolver";

    private ConflictingAccounts conflictingAccounts;

    public ConflictingAccounts getConflictingAccounts() {
        return conflictingAccounts;
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
    public void init(ApiService<A> apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super.init(apiService, originalResponse, loginCallback);
        // Get conflicting accounts.
        GigyaLogger.debug(LOG_TAG, "init: sending fetching conflicting accounts");
        _apiService.send(GigyaDefinitions.API.API_GET_CONFLICTING_ACCOUNTS,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("regToken", _regToken))),
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

    public void resolveForSocial(final Context context, String provider) {
        if (isAttached()) {
            LoginProvider loginProvider = LoginProviderFactory.providerFor(
                    context, _apiService, provider, _loginCallback.get());
            loginProvider.setRegToken(_regToken);
            if (loginProvider.getProviderClientId() == null) {
                final Config config = _apiService.getSessionService().getConfig();
                final Map<String, String> appIds = config.getAppIds();
                if (appIds != null && !appIds.isEmpty()) {
                    for (Map.Entry<String, String> entry : appIds.entrySet()) {
                        final String appProvider = entry.getKey();
                        if (ObjectUtils.safeEquals(appProvider, provider)) {
                            final String appClientId = entry.getValue();
                            loginProvider.updateProviderClientId(appClientId);
                        }
                    }
                }
            }
            Map<String, Object> params = new HashMap<>();
            params.put("provider", provider);
            loginProvider.login(context, params, "link");
        }
    }


    void finalizeFlow(String regToken) {
        if (isAttached()) {
            _apiService.finalizeRegistration(regToken, _loginCallback.get());
        }
    }
}
