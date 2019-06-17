package com.gigya.android.sdk.interruption.link;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts;
import com.gigya.android.sdk.network.GigyaError;

import java.util.HashMap;
import java.util.Map;

public class LinkAccountsResolver<A extends GigyaAccount> extends Resolver<A> implements ILinkAccountsResolver {

    private static final String LOG_TAG = "GigyaLinkAccountsResolver";

    private ConflictingAccounts _conflictingAccounts;

    public LinkAccountsResolver(GigyaLoginCallback<A> loginCallback,
                                GigyaApiResponse interruption,
                                IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }


    @Override
    public ConflictingAccounts getConflictingAccounts() {
        return _conflictingAccounts;
    }


    @Override
    public void requestConflictingAccounts() {
        GigyaLogger.debug(LOG_TAG, "init: sending fetching conflicting accounts");
        // Get conflicting accounts.
        _businessApiService.getConflictingAccounts(getRegToken(), new GigyaCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    _conflictingAccounts = response.getField("conflictingAccount", ConflictingAccounts.class);
                    if (_conflictingAccounts == null) {
                        _loginCallback.onError(GigyaError.generalError());
                    } else {
                        _loginCallback.onConflictingAccounts(_interruption, LinkAccountsResolver.this);
                    }
                } else {
                    _loginCallback.onError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onError(GigyaError error) {
                _loginCallback.onError(error);
            }
        });
    }

    @Override
    public void linkToSite(String loginID, String password) {
        GigyaLogger.debug(LOG_TAG, "linkToSite: with loginID = " + loginID);
        final Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginID);
        params.put("password", password);
        params.put("loginMode", "link");
        params.put("regToken", getRegToken());
        _businessApiService.login(params, _loginCallback);
    }

    @Override
    public void linkToSocial(String providerName) {
        GigyaLogger.debug(LOG_TAG, "linkToSocial: with provider" + providerName);
        final Map<String, Object> params = new HashMap<>();
        params.put("loginMode", "link");
        params.put("regToken", getRegToken());
        _businessApiService.login(providerName, params, _loginCallback);

    }
}
