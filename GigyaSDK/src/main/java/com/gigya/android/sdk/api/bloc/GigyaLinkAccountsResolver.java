package com.gigya.android.sdk.api.bloc;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.services.ApiService;

public class GigyaLinkAccountsResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    GigyaLinkAccountsResolver(ApiService<A> apiService, GigyaApiResponse apiResponse, GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        super(apiService, apiResponse, loginCallback);
    }

    @Override
    protected void init() {

    }

    @Override
    public void clear() {

    }

    public void finalizeFlow() {

    }
}
