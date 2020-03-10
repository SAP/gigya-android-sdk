package com.gigya.android.sdk.interruption;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;

import java.util.HashMap;
import java.util.Map;

public class Resolver<A extends GigyaAccount> {

    final protected GigyaLoginCallback<A> _loginCallback;
    final protected GigyaApiResponse _interruption;
    final protected IBusinessApiService<A> _businessApiService;

    public Resolver(GigyaLoginCallback<A> loginCallback, GigyaApiResponse interruption, IBusinessApiService<A> businessApiService) {
        _loginCallback = loginCallback;
        _interruption = interruption;
        _businessApiService = businessApiService;
    }

    public String getRegToken() {
        return _interruption.getField("regToken", String.class);
    }

    protected void finalizeRegistration(@Nullable Runnable completionHandler) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken()); // Null will result in error.
        params.put("include", "profile,data,emails,subscriptions,preferences");
        params.put("includeUserInfo", "true");
        _businessApiService.finalizeRegistration(params, _loginCallback);
        if (completionHandler != null) {
            completionHandler.run();
        }
    }
}
