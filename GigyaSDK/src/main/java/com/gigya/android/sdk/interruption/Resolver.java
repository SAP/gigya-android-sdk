package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;

public class Resolver<A extends GigyaAccount> {

    final protected GigyaLoginCallback<A> _loginCallback;
    final protected GigyaApiResponse _interruption;
    final protected IBusinessApiService<A> _businessApiService;

    public Resolver(GigyaLoginCallback<A> loginCallback, GigyaApiResponse interruption, IBusinessApiService<A> businessApiService) {
        _loginCallback = loginCallback;
        _interruption = interruption;
        _businessApiService = businessApiService;
    }

}
