package com.gigya.android.sdk.interruption;

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

    protected void finalizeRegistration() {
        final String regToken = _interruption.getField("regToken", String.class);
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken); // Null will result in error.
        params.put("include", "profile,data,emails,subscriptions,preferences");
        params.put("includeUserInfo", "true");
        _businessApiService.finalizeRegistration(params, _loginCallback);
    }
}
