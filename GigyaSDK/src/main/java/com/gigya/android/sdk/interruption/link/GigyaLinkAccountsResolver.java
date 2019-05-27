package com.gigya.android.sdk.interruption.link;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

public class GigyaLinkAccountsResolver<A extends GigyaAccount> extends GigyaResolver<A> implements IGigyaLinkAccountsResolver {

    private static final String LOG_TAG = "GigyaLinkAccountsResolver";

    private ConflictingAccounts _conflictingAccounts;

    public GigyaLinkAccountsResolver(Config config,
                                     ISessionService sessionService,
                                     GigyaApiResponse originalResponse,
                                     IBusinessApiService<A> businessApiService,
                                     GigyaLoginCallback<A> loginCallback) {
        super(config, sessionService, businessApiService, originalResponse, loginCallback);
        requestConflictingAccounts();
    }

    @Override
    public ConflictingAccounts getConflictingAccounts() {
        return _conflictingAccounts;
    }


    @Override
    public void requestConflictingAccounts() {
        GigyaLogger.debug(LOG_TAG, "init: sending fetching conflicting accounts");
        // Get conflicting accounts.
        _businessApiService.getConflictingAccounts(_regToken, new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    _conflictingAccounts = response.getField("conflictingAccount", ConflictingAccounts.class);
                    if (_conflictingAccounts == null) {
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
            public void onError(GigyaError error) {
                forwardError(error);
            }
        });
    }

    @Override
    public void clear() {
        _regToken = null;
        _conflictingAccounts = null;
        if (isAttached()) {
            _loginCallback.clear();
        }
    }

    @Override
    public void linkToSite(String loginID, String password) {
        GigyaLogger.debug(LOG_TAG, "linkToSite: with loginID = " + loginID);
        if (isAttached()) {
            final Map<String, Object> params = new HashMap<>();
            params.put("loginID", loginID);
            params.put("password", password);
            params.put("loginMode", "link");
            params.put("regToken", _regToken);
            _businessApiService.login(params, _loginCallback.get());
        }
    }

    @Override
    public void linkToSocial(String providerName) {
        GigyaLogger.debug(LOG_TAG, "linkToSocial: with provider" + providerName);
        if (isAttached()) {
            final Map<String, Object> params = new HashMap<>();
            params.put("loginMode", "link");
            params.put("regToken", _regToken);
            _businessApiService.login(providerName, params, _loginCallback.get());
        }
    }
}
