package com.gigya.android.sdk.api.interruption;

import android.support.v4.util.Pair;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.ConflictingAccounts;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;

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
        _loginCallback.clear();
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


    void finalizeFlow() {
        if (isAttached()) {
            _apiService.finalizeRegistration(_regToken, _loginCallback.get());
        }
    }
}
